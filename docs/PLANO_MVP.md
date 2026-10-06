# PayFlow — Plano de execução do MVP

## 1. Objetivo

Construir uma base funcional em Java 21 e Spring Boot capaz de criar um pagamento simulado, consultar seu resultado e enviar um webhook ao lojista, sem duplicar a operação quando a requisição for repetida.

O MVP não movimenta dinheiro real nem recebe dados de cartão. Clean Architecture
é aplicada agora à base existente. Vertical Slice e a etapa específica de SOLID
serão feitas por outra equipe.

Este documento é um plano de execução, não uma declaração de funcionalidades já implementadas.

**Decisão atual:** usar MongoDB Atlas como banco externo durável.
A API continua em Java/Spring Boot e pode executar no Azure. JPA, Flyway,
PostgreSQL e H2 não fazem parte da base atual. Transações e idempotência
do fluxo de pagamentos ainda precisam ser implementadas.

### Estado verificado em 05/10/2026

A etapa 1 está concluída: API executada em Docker no macOS Apple Silicon,
conexão ao MongoDB Atlas confirmada pelo health check `UP`, índice único e
provisionamento do lojista implementados. Os três testes de integração com
MongoDB isolado passaram. A imagem atual foi validada em ARM64.

Não há endpoints de pagamentos, autenticação HTTP, eventos, portal ou função
implementados. Não houve publicação da imagem ou implantação da API no Azure.
Windows, Linux e execução AMD64 ainda precisam de validação.

Clean Architecture foi aplicada ao provisionamento de lojista: domínio puro,
caso de uso, contrato de persistência, adaptador MongoDB e composição Spring.
Cinco testes unitários e cinco testes de integração passaram após a refatoração,
incluindo concorrência e rejeição de troca da chave sem sobrescrever os dados.
O núcleo compila com o JDK sem frameworks. O container anterior ainda precisa
ser reconstruído para executar essa versão.

## 2. Escopo

### Incluído no MVP

- Um lojista pré-cadastrado, com autenticação por API Key.
- Criação de pagamento com token de cartão fictício.
- Simulação de aprovação, recusa e timeout do provedor.
- Consulta de pagamento por identificador, limitada ao lojista autenticado.
- Idempotência na criação de pagamentos.
- Persistência em MongoDB Atlas.
- Publicação confiável de eventos com Transactional Outbox.
- Consumo de eventos e envio de webhook por uma função Java.
- Retentativas e dead-letter queue (DLQ).
- Portal simples para criar e consultar pagamentos.
- Execução local e implantação da demonstração no Azure.

### Fora do MVP inicial

- Pagamentos reais, PIX e estorno.
- Cadastro público de lojistas e gestão de usuários.
- Relatórios e painel de administração.
- Histórico completo de entregas e reprocessamento de DLQ pela interface.
- Microfrontends, BFF, API Gateway próprio e extração de Merchant Service.
- Refatoração para Vertical Slice e etapa específica de SOLID.

Os estilos arquiteturais exigidos pela atividade continuam obrigatórios na entrega final. Seu adiamento vale apenas para o primeiro incremento.

## 3. Componentes iniciais

| Componente | Tecnologia | Responsabilidade planejada | Estado atual |
|---|---|---|---|
| Payment API | Java 21 + Spring Boot + Maven | Autenticação, pagamentos, consulta, idempotência e outbox | Base e health check |
| Banco da API | MongoDB Atlas | Lojista, pagamentos, idempotência e eventos | Coleção `merchants` e índice |
| Mensageria | Azure Service Bus | Transporte de eventos, retentativas e DLQ | Não implementada |
| Notification Function | Azure Functions em Java | Consumir eventos e entregar webhooks | Diretório vazio |
| Portal | React + TypeScript | Criar e consultar pagamentos | Diretório vazio |
| Receptor de demonstração | Endpoint local de apoio | Receber webhooks e verificar entrega | Não implementado |

O simulador do provedor começa dentro da API, sem um serviço independente. Redis não faz parte da base inicial.

```text
Portal ou loja integradora
           |
       Payment API
           |
       MongoDB Atlas
       pagamento + outbox
           |
   Publicador da outbox
           |
      Service Bus
           |
   Notification Function
           |
     Webhook do lojista
```

## 4. Organização do código

Estrutura atual da Payment API, após aplicar Clean Architecture à base:

```text
domain/
application/
infrastructure/persistence/mongodb/
config/
```

O domínio `Merchant` e o caso de uso `ProvisionDemoMerchant` não dependem de
frameworks. `MerchantStore` define a persistência necessária ao caso de uso.
O adaptador MongoDB implementa esse contrato e mapeia documentos para o domínio.
Spring compõe as dependências em `config`. Manter um único módulo Maven,
sem organização por slices ou abstrações extras para uma revisão de SOLID.

Manter regras fora dos controllers, validação explícita e responsabilidades compreensíveis. A função terá um trigger simples e lógica de entrega separada o suficiente para ser testada, sem impor uma arquitetura ao próximo time.

Estrutura inicial do repositório:

```text
payment-api/
notification-function/
portal/
```

## 5. Contratos mínimos

### HTTP

| Operação | Endpoint | Comportamento | Estado |
|---|---|---|---|
| Criar pagamento | `POST /v1/payments` | Exige API Key e `Idempotency-Key` | Planejado |
| Consultar pagamento | `GET /v1/payments/{id}` | Retorna apenas pagamento pertencente ao lojista | Planejado |
| Verificar saúde | `/actuator/health` | Verifica aplicação e conectividade MongoDB | Implementado |

A criação recebe valor, moeda, referência da loja e token fictício. A resposta inclui identificador e estado do pagamento.

Usar valores monetários exatos, nunca `float` ou `double`. O MVP aceita apenas BRL e valores positivos. Erros HTTP seguem Problem Details.

Estados iniciais: `PROCESSING`, `AUTHORIZED`, `DECLINED` e `UNKNOWN`. `UNKNOWN` representa resultado incerto após timeout, não uma recusa.

### Idempotência

- Escopo da chave: lojista + operação + `Idempotency-Key`.
- Mesma chave e mesmo conteúdo: devolver a resposta já registrada quando concluída.
- Mesma chave e conteúdo diferente: responder `409 Conflict`.
- Operação ainda em andamento: responder conflito explícito, sem iniciar outra cobrança.
- Garantir unicidade no banco, inclusive com requisições concorrentes.
- Resultado incerto não permite reiniciar automaticamente a cobrança.
- Para a demonstração, manter os registros sem expiração automática; documentar uma política de retenção antes do uso real.

### Evento e webhook

Evento inicial: `payment.status-changed.v1`.

Campos mínimos: `eventId`, `eventType`, `schemaVersion`, `occurredAt`, `paymentId`, `merchantId`, `paymentVersion` e `status`.

- Evento e alteração do pagamento são persistidos na mesma transação.
- O publicador marca a outbox como publicada somente após confirmação do broker.
- O processamento da outbox deve coordenar réplicas concorrentes.
- A entrega é pelo menos uma vez: eventos e webhooks podem se repetir.
- O receptor deve deduplicar por `eventId`; não prometer entrega exatamente uma vez.
- Assinar o webhook com HMAC-SHA256 e timestamp.
- Marcar a mensagem como concluída somente após resposta HTTP 2xx.
- Configurar timeout, retentativas limitadas e encaminhamento à DLQ.
- Não registrar tokens, API Keys ou segredos nos logs.

A função inicial não terá banco próprio nem promessa de histórico ou deduplicação durável. Para a demonstração, a configuração do único destinatário e seu segredo são fornecidos ao ambiente da função, sem colocá-los no evento.

## 6. Persistência inicial

| Coleção | Conteúdo |
|---|---|
| `merchants` | Lojista pré-cadastrado e hash da API Key |
| `payments` | Valor, moeda, referência, estado e versão |
| `idempotency_records` | Escopo, chave, hash da requisição, estado e resposta |
| `outbox_events` | Evento, payload, estado de publicação e tentativas |

Somente `merchants` está implementada. As demais coleções são planejadas.

Usar Spring Data MongoDB e índices explícitos. A base cria um índice único para
o hash de API Key. Versionar futuras evoluções dos documentos e scripts de
migração; Flyway e Hibernate não são usados. O banco deve suportar transações
multi-documento para a futura outbox; implementar e validar o gerenciamento
de transações MongoDB, sem presumir atomicidade entre coleções.

Antes de simular a chamada ao provedor, persistir a identidade da operação. Depois, atualizar o resultado e inserir o evento em uma transação. Não manter uma transação de banco aberta durante uma chamada externa.

No simulador, usar o identificador estável da operação para demonstrar idempotência. Em integração real, esse contrato dependerá das capacidades do provedor e precisará incluir reconciliação.

## 7. Ambientes e segurança

### Local

- Docker Compose para a API conectada ao Atlas; receptor de webhook a implementar.
- API e portal executados localmente.
- Azure Functions Core Tools para a função.
- Avaliar o emulador oficial de Service Bus e sua compatibilidade com o ambiente de desenvolvimento.
- Se o emulador não for viável, usar um namespace de desenvolvimento no Azure, com dados sintéticos.
- Não substituir a mensageria por um mock e considerar o fluxo ponta a ponta validado.

### Azure

- Payment API no Azure Container Apps.
- Notification Function no Azure Functions.
- Service Bus para mensagens e DLQ.
- MongoDB Atlas externo, com credenciais como segredo e acesso de rede restrito.
- Portal em hospedagem estática, não necessariamente Azure.
- Credenciais fornecidas pelo ambiente; usar identidade gerenciada onde aplicável.
- Confirmar planos, região, permissões e custos antes de provisionar.

O portal não deve conter API Key ou outros segredos no bundle. Para o MVP, a credencial de demonstração pode ser informada em tempo de execução e mantida apenas em memória. Isso não substitui a autenticação do portal e o BFF da entrega final.

## 8. Etapas de execução

### Etapa 1 — Preparar a base

- [x] Criar a aplicação Spring Boot com Maven e Java 21.
- [x] Criar os diretórios dos componentes.
- [x] Configurar integração MongoDB e variáveis de ambiente de exemplo, sem segredos reais.
- [x] Criar índice único e provisionamento do lojista de demonstração.
- [x] Expor health check.

Critério de conclusão: API inicia, conecta ao banco e cria o índice e o lojista.
A conexão ao cluster Atlas foi verificada na execução Docker atual. Cada membro
do time precisa configurar suas credenciais e acesso de rede sem versionar segredos.

### Etapa 2 — Implementar pagamentos

- [ ] Implementar autenticação por API Key.
- [ ] Implementar criação e consulta com isolamento por lojista.
- [ ] Implementar simulador de aprovação, recusa e timeout.
- [ ] Implementar idempotência persistida e controle de concorrência.
- [ ] Padronizar validação e respostas de erro.

Critério de conclusão: criar e consultar pagamentos; repetição não cria outra operação; payload conflitante retorna 409; timeout fica explicitamente incerto.

### Etapa 3 — Implementar eventos e notificações

- [ ] Persistir outbox junto às mudanças de estado.
- [ ] Implementar publicador e configurar Service Bus.
- [ ] Implementar função consumidora.
- [ ] Implementar assinatura e entrega do webhook.
- [ ] Configurar retentativas e DLQ.
- [ ] Criar receptor de demonstração com deduplicação por `eventId`.

Critério de conclusão: webhook chega ao receptor; falhas são recuperáveis; esgotamento das tentativas envia a mensagem à DLQ.

### Etapa 4 — Implementar o portal

- [ ] Criar formulário de pagamento com aviso de simulação.
- [ ] Criar consulta e visualização do estado.
- [ ] Tratar carregamento, erros e repetição de requisição.
- [ ] Restringir CORS aos endereços necessários.

Critério de conclusão: executar o fluxo pelo navegador sem credenciais embutidas no frontend.

### Etapa 5 — Implantar e demonstrar

- [x] Criar imagem da API e configuração de implantação.
- [ ] Provisionar recursos Azure compatíveis com o orçamento.
- [ ] Implantar API, função e portal.
- [ ] Verificar logs com identificadores de correlação, sem dados sensíveis.
- [ ] Executar os critérios de aceite no ambiente implantado.
- [ ] Documentar execução local, configuração, limitações e desligamento dos recursos.

Critério de conclusão: demonstração ponta a ponta reproduzível e instruções persistidas no repositório.

## 9. Validação obrigatória

- [ ] API Key ausente ou inválida é rejeitada.
- [ ] Lojista não acessa pagamento de outro lojista.
- [ ] Valor inválido e moeda não suportada são rejeitados.
- [ ] Aprovação e recusa produzem estados corretos.
- [ ] Repetições concorrentes não criam pagamentos duplicados.
- [ ] Chave reutilizada com payload diferente retorna conflito.
- [ ] Timeout não produz recusa falsa nem nova cobrança automática.
- [ ] Falha do broker mantém evento pendente na outbox.
- [ ] Reinício do publicador e da API retoma eventos pendentes no banco externo.
- [ ] Webhook contém assinatura válida e identificador estável.
- [ ] Falha de entrega produz retentativas e DLQ.
- [ ] Reentrega é tolerada pelo receptor.
- [ ] Fluxo funciona no Azure, não apenas com dependências simuladas.

Usar testes unitários para regras e testes de integração com MongoDB em replica
set para transações e concorrência. Validar persistência após reinício e
separadamente a conectividade com Atlas.
Validar separadamente a integração real com Service Bus e Azure Functions.

## 10. Evolução e passagem para o outro time

Após a aceitação da base:

1. Dividir o portal em dois microfrontends: pagamentos e configuração da integração.
2. Acrescentar Spring Cloud Gateway e BFF específico do portal.
3. Extrair Merchant Service com banco e usuário próprios.
4. Evoluir configuração de destinatários e persistência de notificações, se exigidas.
5. Entregar ao outro time os contratos, índices, evolução dos documentos, testes, cenários de falha e instruções de execução.
6. O outro time aplica Vertical Slice e SOLID, preservando os limites de Clean Architecture, contratos e comportamentos acordados.

Database per Service significa propriedade exclusiva dos dados: nenhum serviço
consulta diretamente as coleções de outro. Serviços podem compartilhar um cluster
Atlas com bancos e usuários separados, respeitando os limites do plano.

Clean Architecture está limitada ao provisionamento de lojista existente.
Não apresentar essa entrega como Vertical Slice nem como MVP completo.

## 11. Documentação da entrega final

- [ ] Revisar as decisões AWS do PDF anterior para a implantação Azure.
- [ ] Registrar ADRs para persistência, hospedagem, eventos, divisão dos serviços e limites do MVP.
- [ ] Manter C4 níveis 1 e 2 conforme a estrutura realmente implantada.
- [ ] Atualizar C4 níveis 3 e 4 depois da refatoração do outro time.
- [ ] Entregar UML de classes, componentes e sequência.
- [ ] Entregar modelo lógico de entidades e relacionamentos por serviço, explicando sua representação documental no MongoDB, sem chaves estrangeiras físicas entre bancos.
- [ ] Atualizar Software Architecture Canvas.
- [ ] Usar o template oficial arc42 e inserir diagramas nas respectivas seções.
- [ ] Gerar PDF e verificar legibilidade, cortes e referências.
- [ ] Confirmar com o professor a divergência entre as 11 seções solicitadas e as 12 do template oficial, incluindo Glossário.

A documentação deve diferenciar claramente o que foi implementado, o que foi validado e o que permanece planejado.

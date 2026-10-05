# PayFlow — Plano de execução do MVP

## 1. Objetivo

Construir uma base funcional em Java 21 e Spring Boot capaz de criar um pagamento simulado, consultar seu resultado e enviar um webhook ao lojista, sem duplicar a operação quando a requisição for repetida.

O MVP não movimenta dinheiro real, não recebe dados de cartão e não implementa Vertical Slice, Clean Architecture ou uma estrutura orientada a SOLID. Outro time será responsável por essa evolução.

Este documento é um plano de execução, não uma declaração de funcionalidades já implementadas.

## 2. Escopo

### Incluído no MVP

- Um lojista pré-cadastrado, com autenticação por API Key.
- Criação de pagamento com token de cartão fictício.
- Simulação de aprovação, recusa e timeout do provedor.
- Consulta de pagamento por identificador, limitada ao lojista autenticado.
- Idempotência na criação de pagamentos.
- Persistência em PostgreSQL.
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
- Refatoração para Vertical Slice, Clean Architecture e SOLID.

Os estilos arquiteturais exigidos pela atividade continuam obrigatórios na entrega final. Seu adiamento vale apenas para o primeiro incremento.

## 3. Componentes iniciais

| Componente | Tecnologia | Responsabilidade |
|---|---|---|
| Payment API | Java 21 + Spring Boot + Maven | Autenticação, pagamentos, consulta, idempotência e outbox |
| Banco da API | PostgreSQL | Lojista pré-cadastrado, pagamentos, idempotência e eventos pendentes |
| Mensageria | Azure Service Bus | Transporte de eventos, retentativas e DLQ |
| Notification Function | Azure Functions em Java | Consumir eventos e entregar webhooks |
| Portal | React + TypeScript | Criar e consultar pagamentos |
| Receptor de demonstração | Endpoint local de apoio | Receber webhooks e permitir verificar a entrega |

O simulador do provedor começa dentro da API, sem um serviço independente. Redis não faz parte da base inicial.

```text
Portal ou loja integradora
           |
       Payment API
           |
       PostgreSQL
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

Usar uma estrutura convencional na Payment API:

```text
controller/
service/
repository/
entity/
dto/
integration/
config/
```

Não criar handlers por slice, portas e adaptadores, módulos por camada ou interfaces apenas para antecipar a refatoração.

Manter regras fora dos controllers, validação explícita e responsabilidades compreensíveis. A função terá um trigger simples e lógica de entrega separada o suficiente para ser testada, sem impor uma arquitetura ao próximo time.

Estrutura inicial do repositório:

```text
payment-api/
notification-function/
portal/
local/
```

## 5. Contratos mínimos

### HTTP

| Operação | Endpoint | Comportamento |
|---|---|---|
| Criar pagamento | `POST /v1/payments` | Exige API Key e `Idempotency-Key` |
| Consultar pagamento | `GET /v1/payments/{id}` | Retorna apenas pagamento pertencente ao lojista |
| Verificar saúde | `/actuator/health` | Usado para verificar a aplicação |

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

| Tabela | Conteúdo |
|---|---|
| `merchants` | Lojista pré-cadastrado e hash da API Key |
| `payments` | Valor, moeda, referência, estado e versão |
| `idempotency_records` | Escopo, chave, hash da requisição, estado e resposta |
| `outbox_events` | Evento, payload, estado de publicação e tentativas |

Usar migrações versionadas, por exemplo com Flyway. Não depender de criação automática de tabelas pelo Hibernate nos ambientes compartilhados.

Antes de simular a chamada ao provedor, persistir a identidade da operação. Depois, atualizar o resultado e inserir o evento em uma transação. Não manter uma transação de banco aberta durante uma chamada externa.

No simulador, usar o identificador estável da operação para demonstrar idempotência. Em integração real, esse contrato dependerá das capacidades do provedor e precisará incluir reconciliação.

## 7. Ambientes e segurança

### Local

- Docker Compose para PostgreSQL e receptor de webhook.
- API e portal executados localmente.
- Azure Functions Core Tools para a função.
- Avaliar o emulador oficial de Service Bus e sua compatibilidade com o ambiente de desenvolvimento.
- Se o emulador não for viável, usar um namespace de desenvolvimento no Azure, com dados sintéticos.
- Não substituir a mensageria por um mock e considerar o fluxo ponta a ponta validado.

### Azure

- Payment API no Azure Container Apps.
- Notification Function no Azure Functions.
- Service Bus para mensagens e DLQ.
- PostgreSQL preferencialmente gerenciado.
- Portal em hospedagem estática, não necessariamente Azure.
- Credenciais fornecidas pelo ambiente; usar identidade gerenciada onde aplicável.
- Confirmar planos, região, permissões e custos antes de provisionar.

O portal não deve conter API Key ou outros segredos no bundle. Para o MVP, a credencial de demonstração pode ser informada em tempo de execução e mantida apenas em memória. Isso não substitui a autenticação do portal e o BFF da entrega final.

## 8. Etapas de execução

### Etapa 1 — Preparar a base

- [ ] Criar a aplicação Spring Boot com Maven e Java 21.
- [ ] Criar os diretórios dos componentes.
- [ ] Configurar PostgreSQL local e variáveis de ambiente de exemplo, sem segredos reais.
- [ ] Criar migrações e provisionamento do lojista de demonstração.
- [ ] Expor health check.

Critério de conclusão: API inicia, conecta ao banco e aplica as migrações.

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

- [ ] Criar imagem da API e configuração de implantação.
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
- [ ] Reinício do publicador retoma eventos pendentes.
- [ ] Webhook contém assinatura válida e identificador estável.
- [ ] Falha de entrega produz retentativas e DLQ.
- [ ] Reentrega é tolerada pelo receptor.
- [ ] Fluxo funciona no Azure, não apenas com dependências simuladas.

Usar testes unitários para regras e testes de integração com PostgreSQL para persistência, transações e concorrência. Validar separadamente a integração real com Service Bus e Azure Functions.

## 10. Evolução e passagem para o outro time

Após a aceitação da base:

1. Dividir o portal em dois microfrontends: pagamentos e configuração da integração.
2. Acrescentar Spring Cloud Gateway e BFF específico do portal.
3. Extrair Merchant Service com banco e usuário próprios.
4. Evoluir configuração de destinatários e persistência de notificações, se exigidas.
5. Entregar ao outro time os contratos, migrações, testes, cenários de falha e instruções de execução.
6. O outro time aplica Vertical Slice, Clean Architecture e SOLID preservando os contratos e comportamentos acordados.

Database per Service significa propriedade exclusiva dos dados: nenhum serviço consulta diretamente as tabelas de outro. Bancos separados podem compartilhar uma instância PostgreSQL, com permissões isoladas.

Não apresentar a organização convencional do MVP como implementação de Clean Architecture ou Vertical Slice.

## 11. Documentação da entrega final

- [ ] Revisar as decisões AWS do PDF anterior para a implantação Azure.
- [ ] Registrar ADRs para persistência, hospedagem, eventos, divisão dos serviços e limites do MVP.
- [ ] Manter C4 níveis 1 e 2 conforme a estrutura realmente implantada.
- [ ] Atualizar C4 níveis 3 e 4 depois da refatoração do outro time.
- [ ] Entregar UML de classes, componentes e sequência.
- [ ] Entregar DER por serviço, sem relacionamentos físicos entre bancos.
- [ ] Atualizar Software Architecture Canvas.
- [ ] Usar o template oficial arc42 e inserir diagramas nas respectivas seções.
- [ ] Gerar PDF e verificar legibilidade, cortes e referências.
- [ ] Confirmar com o professor a divergência entre as 11 seções solicitadas e as 12 do template oficial, incluindo Glossário.

A documentação deve diferenciar claramente o que foi implementado, o que foi validado e o que permanece planejado.

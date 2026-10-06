# Publicacao Docker

## Estado da entrega

API executada em Docker ARM64 no macOS com MongoDB Atlas e health check `UP`
em 05/10/2026. Build e execucao da imagem atual em AMD64, publicacao no registry
e implantacao Azure ainda estao pendentes. Os comandos abaixo sao instrucoes,
nao registro de recursos ja provisionados.

## Imagem

A API usa MongoDB Atlas externo ao container. Configure `MONGODB_URI` como
segredo e `MONGODB_DATABASE` (padrao `payflow`).
Nao use as antigas variaveis `DATABASE_URL`, `DATABASE_USERNAME` ou `DATABASE_PASSWORD`.
Nao ative o perfil `local` na nuvem.

Build local, na raiz:

```bash
docker build --tag payflow/payment-api:dev ./payment-api
```

O Dockerfile compila com Java 21 e Maven Wrapper e executa somente o JRE,
como usuario nao root. Testes sao executados separadamente com `mvnw verify`;
o build da imagem nao os executa.

O Compose executa apenas a API. Nao existe container de banco nem volume
de dados no projeto; a persistencia fica no Atlas.

## Registry

Pode usar Docker Hub, GitHub Container Registry ou Azure Container Registry.
Faca login pelo fluxo seguro do registry, sem versionar tokens:

```bash
docker login SEU_REGISTRY
```

Para ACR existente e Azure CLI autenticada: `az acr login --name NOME_DO_ACR`.

Substitua a tag abaixo pelo repositorio real:

```bash
docker buildx create --name payflow-builder --driver docker-container
docker buildx build --builder payflow-builder --platform linux/amd64,linux/arm64 --tag SEU_REGISTRY/payflow/payment-api:0.1.0 --push ./payment-api
docker buildx imagetools inspect SEU_REGISTRY/payflow/payment-api:0.1.0
```

Crie o builder apenas uma vez. No Docker Hub, a tag pode ser
`SEU_USUARIO/payment-api:0.1.0`; no ACR,
`NOME_DO_ACR.azurecr.io/payflow/payment-api:0.1.0`.

Para usar uma imagem publicada pelo Compose, defina `PAYMENT_API_IMAGE`
no `.env` e execute:

```bash
docker compose --env-file .env pull
docker compose --env-file .env up -d --no-build
```

## Azure Container Apps ou outra plataforma

- Configure a imagem e acesso ao registry privado, se necessario.
- Configure ingress HTTPS para a porta 8080.
- Configure startup/readiness em `/actuator/health`.
- Habilite `MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED=true` para usar
  `/actuator/health/liveness` como liveness.
- Libere no Atlas o IP de saida do ambiente ou configure conectividade privada
  suportada pelo seu plano. O endereco de ingress da API nao e seu IP de saida.
- Garanta resolucao DNS SRV e acesso de rede aos hosts do cluster.
- Para provisionar o lojista, forneca `PAYFLOW_DEMO_MERCHANT_ENABLED=true`
  e `PAYFLOW_DEMO_MERCHANT_API_KEY` como segredo da plataforma.
- Nao monte `.env` no container nem inclua credenciais na imagem.

Os dados permanecem no Atlas apos reinicios e trocas de imagem. Configure
backups e retencao conforme as capacidades e custos do plano escolhido.
Ainda nao ha fluxo de pagamentos implementado; persistencia externa por si
so nao torna a aplicacao adequada a operacoes financeiras reais.

Para a futura outbox e idempotencia, sera necessario implementar transacoes
MongoDB, indices unicos, coordenacao entre replicas e testes de concorrencia.
A base atual so provisiona um documento de lojista de forma atomica.

O Compose vincula a API a `127.0.0.1`; nao publica HTTPS na internet.
Em servidor proprio, use proxy reverso, TLS e firewall.

Recursos ja criados no Azure nao foram alterados ou excluidos; revise seus
custos separadamente.

## Checklist de publicacao

- [ ] Executar os testes e construir a imagem da versao a entregar.
- [ ] Publicar e inspecionar a imagem AMD64/ARM64 no registry escolhido.
- [ ] Configurar segredos, conectividade Atlas e ingress HTTPS na plataforma.
- [ ] Verificar health check e persistencia apos reinicio na nuvem.
- [ ] Confirmar custos, controles de acesso e politica de backup.
- [ ] Implementar e validar os fluxos funcionais antes de declarar o MVP pronto.

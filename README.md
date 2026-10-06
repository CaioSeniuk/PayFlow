# PayFlow

Base de pagamentos simulados com Java 21, Spring Boot e **MongoDB Atlas**.
Nao processa dinheiro real nem recebe dados de cartao.

## Estado atual

Spring Data MongoDB, documento e repositorio de lojistas, indice unico para
hash da API Key, provisionamento opcional de demonstracao e health check.
Pagamentos, autenticacao HTTP, portal e notificacoes ainda nao foram implementados.
JPA, Flyway, PostgreSQL e H2 foram removidos da configuracao atual.

Organizacao convencional, sem Vertical Slice ou Clean Architecture.
Veja [PLANO_MVP.md](PLANO_MVP.md) para o escopo.

### Validacao realizada

Em 05/10/2026, a API foi executada em Docker no macOS Apple Silicon,
conectada ao MongoDB Atlas, e `/actuator/health` retornou `UP`.
Tres testes de integracao passaram com MongoDB isolado: provisionamento
repetivel com armazenamento apenas do hash, rejeicao de hash duplicado e
health check sem detalhes internos. A imagem atual foi construida e
executada em ARM64; Windows, Linux e runtime AMD64 ainda nao foram testados.

A imagem nao foi publicada em registry e a API nao foi implantada no Azure.
Rodar o container localmente com Atlas nao publica a API na internet.

### Estrutura entregue

```text
payment-api/            API Spring Boot, testes e Dockerfile
notification-function/ Reservado; ainda vazio
portal/                Reservado; ainda vazio
compose.yaml           Executa a API com banco externo
.env.example           Modelo de configuracao sem credenciais
```

## Preparar MongoDB Atlas

1. Crie ou selecione um cluster Atlas, verificando custos e limites.
2. Em Database Access, crie um usuario exclusivo com permissao `readWrite`
   apenas no banco `payflow`. Esse usuario nao e o login do portal Atlas.
3. Em Network Access, permita apenas o IP de saida do ambiente que executa a API.
   Nao use `0.0.0.0/0` para contornar problemas de conexao.
4. Em Connect / Drivers, copie a URI de conexao.

Na primeira configuracao, copie `.env.example` para `.env` na raiz.
Se ja existir um arquivo configurado, nao o sobrescreva:

```bash
cp .env.example .env
```

No PowerShell: `Copy-Item .env.example .env`.
Preencha o arquivo sem compartilhar credenciais:

```dotenv
MONGODB_URI=mongodb+srv://USUARIO:SENHA_CODIFICADA@SEU_CLUSTER.mongodb.net/?retryWrites=true&w=majority
MONGODB_DATABASE=payflow
DEMO_MERCHANT_API_KEY=CHAVE_ALEATORIA_DE_32_A_256_CARACTERES
API_PORT=8080
PAYMENT_API_IMAGE=payflow/payment-api:dev
```

Substitua os placeholders usando os dados reais do Atlas. Caracteres especiais
no usuario/senha precisam ser percent-encoded conforme o formato de URI.
Nao desabilite TLS ou a verificacao de certificado: `mongodb+srv` usa TLS.
Nao versione `.env` nem coloque a URI nos logs, no frontend ou na imagem.

Gere uma API Key com `openssl rand -hex 32` no macOS/Linux.
No Windows PowerShell:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $rng.GetBytes($bytes)
    ([BitConverter]::ToString($bytes)).Replace("-", "").ToLowerInvariant()
} finally {
    $rng.Dispose()
}
```

Se o `.env` ja existir, preserve a API Key e adicione as variaveis MongoDB.
As antigas variaveis PostgreSQL nao sao mais usadas.

### Credenciais diferentes

| Configuracao | Finalidade |
|---|---|
| `MONGODB_URI` | Usuario e senha de banco do Atlas, com o endereco do cluster |
| `MONGODB_DATABASE` | Banco da aplicacao; padrao `payflow` |
| `DEMO_MERCHANT_API_KEY` | Chave propria do lojista PayFlow; nao e gerada no Atlas |

As chaves Public/Private da API administrativa do Atlas nao sao usadas.
Se uma senha ou chave privada for exposta em chat, imagem ou repositorio,
revogue ou rotacione a credencial correspondente antes de usa-la.

## Docker: macOS, Windows e Linux

Requer Docker com Compose v2 ou superior e Linux containers.
No Windows, use Docker Desktop com WSL2. Nao precisa Java ou Maven no host.

```bash
docker compose --env-file .env up -d --build
docker compose logs -f payment-api
```

Aguarde a inicializacao e abra http://localhost:8080/actuator/health.
O campo `status` deve ser `UP`. O health check inclui conectividade MongoDB.
Para parar: `docker compose stop`.

Para verificar pelo terminal:

```bash
curl --fail http://localhost:8080/actuator/health
```

No PowerShell: `Invoke-RestMethod http://localhost:8080/actuator/health`.
O resultado pode incluir os grupos `liveness` e `readiness`, alem de `status`.
Se mudar `API_PORT` no `.env`, ajuste a porta no endereco acima.

O banco esta fora do container e os dados sobrevivem ao reinicio da API.
Uma falha de conexao ou de criacao do indice impede a inicializacao, em vez
de criar silenciosamente um banco local.

## Executar sem Docker

Instale JDK 21 e configure `JAVA_HOME`. O Maven Wrapper esta incluido.
Prepare o `.env` como acima. No macOS/Linux:

```bash
cd payment-api
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

No Windows PowerShell:

```powershell
Set-Location payment-api
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

No macOS, se necessario, selecione Java antes:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
```

O perfil `local` le o `.env` na pasta pai. Fora dele, forneca `MONGODB_URI`
e opcionalmente `MONGODB_DATABASE` pelo ambiente.
Para criar o lojista, forneca `PAYFLOW_DEMO_MERCHANT_ENABLED=true` e
`PAYFLOW_DEMO_MERCHANT_API_KEY` como segredo.

O lojista tem ID `00000000-0000-0000-0000-000000000001` e somente o hash
SHA-256 da chave e armazenado. Reinicios nao duplicam nem trocam sua chave.
Se o lojista ja existir com outra chave, a API falha: restaure a chave original.
Provisionamento nao implementa autenticacao HTTP.

## Solucao de problemas

| Sintoma | Verificacao |
|---|---|
| Compose informa variavel obrigatoria vazia | Preencha `MONGODB_URI` e `DEMO_MERCHANT_API_KEY`; placeholders nao sao valores validos |
| Falha de autenticacao MongoDB | Confira usuario de Database Access, senha atual e percent-encoding |
| Timeout de conexao | Confira IP de saida autorizado, DNS SRV, firewall e disponibilidade do cluster |
| Erro de chave do lojista diferente | Restaure a API Key original; mudar o `.env` nao rotaciona a chave persistida |
| Porta 8080 ocupada | Configure outra `API_PORT` e recrie o container |

Consulte `docker compose logs -f payment-api` localmente. Nao compartilhe logs
ou saidas de `docker compose config` sem remover credenciais.

## Testes

Com Java 21 e Docker em execucao, dentro de `payment-api/`:

```bash
./mvnw verify
```

No PowerShell: `.\mvnw.cmd verify`.
Os testes usam MongoDB real isolado via Testcontainers, sem conectar ao Atlas
ou modificar seus dados. Testar MongoDB local nao valida rede, credenciais
ou limites do seu cluster Atlas.

## Publicacao

Consulte [DEPLOYMENT.md](DEPLOYMENT.md) para publicar a imagem e executar
na nuvem. O banco pode permanecer no Atlas enquanto a API executa no Azure.

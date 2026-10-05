# PayFlow

Base de um MVP de pagamentos simulados em Java 21 e Spring Boot.
Nao processa dinheiro real nem recebe dados de cartao.

## Estado atual

A etapa 1 inclui API, PostgreSQL local, migracao Flyway, lojista de demonstracao
e health check. Endpoints de pagamentos, autenticacao das requisicoes, portal,
mensageria e funcao de notificacoes ainda nao estao implementados.

A estrutura e convencional; Vertical Slice, Clean Architecture e SOLID serao
trabalhados pelo outro time. O escopo e os proximos incrementos estao em
[PLANO_MVP.md](PLANO_MVP.md).

## Requisitos

- JDK 21, com `JAVA_HOME` apontando para sua instalacao.
- Docker com Compose v2 ou superior e daemon em execucao.
- Acesso a internet para baixar dependencias e imagens na primeira execucao.

O Maven Wrapper esta incluido: `mvnw` para macOS/Linux e `mvnw.cmd` para Windows.
Nao e necessario instalar Maven separadamente.

| Sistema | Ambiente necessario |
|---|---|
| macOS Intel ou Apple Silicon | JDK 21 e Docker Desktop |
| Windows 10/11 | JDK 21, Windows PowerShell e Docker Desktop com WSL2 e Linux containers |
| Linux x86_64 ou ARM64 | JDK 21 e Docker Engine com plugin Compose, ou Docker Desktop |

O PostgreSQL usa imagem Linux multi-arquitetura e volume gerenciado pelo Docker,
sem caminhos de arquivos especificos do sistema operacional. No Linux, seu
usuario precisa conseguir executar `docker info` sem `sudo`. No Windows, use
Linux containers, nao Windows containers.

Execute os comandos abaixo na raiz do projeto, salvo quando houver `cd`.

## Configurar e executar localmente

### macOS e Linux (Bash ou Zsh)

```bash
cp .env.example .env
```

Preencha `POSTGRES_PASSWORD` e `DEMO_MERCHANT_API_KEY` no `.env` com valores
locais diferentes. Gere cada valor com `openssl rand -hex 32`. A API Key deve
ter entre 32 e 256 caracteres. Nao compartilhe nem versione o `.env`.

```bash
docker compose --env-file .env -f local/compose.yaml up -d --wait
cd payment-api
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

O perfil `local` le o `.env` da pasta pai automaticamente. Nao e necessario
usar `source` nem exportar suas variaveis. Execute o Maven dentro de
`payment-api/`. Use valores hexadecimais sem aspas no arquivo, para manter
compatibilidade entre a leitura pelo Spring e pelo Compose.

Se o wrapper nao tiver permissao de execucao apos a copia do projeto,
execute `chmod +x payment-api/mvnw` na raiz.

No macOS, se houver varios JDKs, selecione Java 21 antes de executar o Maven:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
```

No Linux, configure `JAVA_HOME` com o caminho do seu JDK 21 e inclua
`$JAVA_HOME/bin` no `PATH`. Em ambos, confira `java -version` e `./mvnw -v`
(o segundo dentro de `payment-api/`).

### Windows (PowerShell)

```powershell
Copy-Item .env.example .env
```

Para gerar cada um dos dois valores, execute o bloco abaixo separadamente:

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

Preencha `POSTGRES_PASSWORD` e `DEMO_MERCHANT_API_KEY` no `.env`, sem aspas.
Configure `JAVA_HOME` para seu JDK 21 se ainda nao estiver configurado:

```powershell
$env:JAVA_HOME = "C:\caminho\para\seu\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
docker compose --env-file .env -f local/compose.yaml up -d --wait
Set-Location payment-api
.\mvnw.cmd -v
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

O caminho do JDK acima e apenas um exemplo; substitua pelo caminho real.
Nao use `source`, `export` ou `/usr/libexec/java_home` no PowerShell.

### Consultar a API

O PostgreSQL fica acessivel somente em `127.0.0.1`, na porta 5433 por padrao,
para evitar conflito com bancos existentes. A API usa a porta 8080.

Em outro terminal, no macOS/Linux:

```bash
curl --fail http://localhost:8080/actuator/health
```

No Windows PowerShell:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Resposta esperada: campo `status` igual a `UP`, sem detalhes internos.
O Actuator tambem pode incluir os nomes dos grupos `liveness` e `readiness`.

O lojista de demonstracao tem ID `00000000-0000-0000-0000-000000000001`.
Somente o hash SHA-256 da chave e armazenado. Reinicios nao duplicam o lojista.
Uma chave diferente para um lojista ja provisionado impede a inicializacao:
restaure a chave original; alterar a variavel nao e uma operacao de rotacao.

O perfil `local` habilita esse provisionamento. Fora dele, configure
`DATABASE_URL`, `DATABASE_USERNAME` e `DATABASE_PASSWORD`; o lojista de
demonstracao nao e criado automaticamente.

## Verificar a base

Com Docker em execucao:

```bash
cd payment-api
./mvnw verify
```

No Windows PowerShell, entre em `payment-api/` e execute `.\mvnw.cmd verify`.

Os testes usam PostgreSQL real e isolado via Testcontainers, sem depender
do `.env` nem modificar o banco de desenvolvimento.

## Compatibilidade verificada

Os wrappers, configuracoes e comandos foram preparados para macOS, Windows
e Linux. A execucao local foi verificada em macOS Apple Silicon com Java 21
e PostgreSQL em Docker. Windows e Linux ainda precisam de validacao de
execucao nesses sistemas; compatibilidade prevista nao equivale a teste realizado.

Essa avaliacao cobre somente a base atual. A compatibilidade do emulador
Service Bus e das ferramentas Azure Functions sera avaliada quando esses
componentes forem implementados.

## Parar

Interrompa a API com Ctrl+C e, na raiz, execute:

```bash
docker compose --env-file .env -f local/compose.yaml stop
```

O volume do PostgreSQL e preservado. Nao execute remocao de volumes se
precisar manter os dados ou o lojista provisionado.

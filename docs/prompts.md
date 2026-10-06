# Resumo dos prompts que levaram a mudanças no projeto

Registro cronológico da construção do PayFlow. As escolhas temporárias são identificadas como parte do histórico, não como configuração atual. Nenhuma credencial é reproduzida.

| # | Prompt resumido | Resultado |
|---|---|---|
| 1 | Ler a documentação TDE01 e discutir o projeto em Java/Spring Boot, considerando os estilos arquiteturais da atividade | Definição da evolução do PayFlow e dos requisitos de arquitetura |
| 2 | “Devemos utilizar o Azure como Cloud” | Azure definido como destino de implantação |
| 3 | “Não precisa ser tudo da Azure” | Permissão para combinar Azure com tecnologias e serviços externos |
| 4 | Enxugar o MVP e deixar Vertical Slice, Clean Architecture e SOLID para outro time | Escopo inicial reduzido e estrutura convencional |
| 5 | “Crie um .md para começarmos a executar o plano” | Criação de `PLANO_MVP.md` |
| 6 | “Vamos começar fazer” | Base Spring Boot/Java 21, Maven Wrapper, PostgreSQL, Flyway, lojista de demonstração e health check |
| 7 | Verificar compatibilidade com macOS, Windows e Linux | Ajustes no carregamento do `.env`, instruções PowerShell e padronização de terminações de linha |
| 8 | Fazer commit e resumir pendências para o outro time | Commit inicial `75c1654` e relatório de implementação pendente |
| 9 | Configurar Docker para publicar a imagem e executar em qualquer ambiente, com banco externo | Dockerfile, Compose e guia de publicação; preparação para AMD64/ARM64 |
| 10 | “Vamos manter o banco em memória” | Troca temporária para H2 e simplificação da execução |
| 11 | “Cancela tudo, vamos utilizar o Atlas MongoDB” | Substituição de H2/PostgreSQL/JPA/Flyway por Spring Data MongoDB, persistência documental e índice único |
| 12 | “Cadê o .env” | Criação do `.env` local, ignorado pelo Git |
| 13 | Informar a connection string do Atlas | Configuração da URI no `.env` com placeholder de senha |
| 14 | Confirmar preenchimento da senha e da chave do lojista | Inicialização via Docker e confirmação da conexão Atlas pelo health check |
| 15 | “Atualize a documentação do projeto” | Atualização de `README.md`, `DEPLOYMENT.md` e `PLANO_MVP.md` |
| 16 | Commit na `main`, push e sincronização da outra branch | Commit `e779605`, publicado em `main` e `clean-architecture-implementation` |
| 17 | Implementar somente Clean Architecture; SOLID e Vertical Slice ficam com outra equipe | Definição do escopo exclusivo da refatoração |
| 18 | “Pode começar” | Separação de domínio, aplicação, contrato de persistência, adaptador MongoDB e composição Spring; novos testes |
| 19 | “Faça commit e push” | Commit `7fb4582`, publicado em `clean-architecture-implementation` |
| 20 | Implementar Vertical Slice e SOLID em duas funcionalidades iniciais e gerar documento com GitHub da nova branch, prompts e diagramas de classes/componentes do backend | Branch `feature/vertical-slice-solid`; fatias `provision` e `authenticate` (`GET /v1/merchants/me`); portas segregadas; ArchUnit; `docs/ARQUITETURA_BACKEND.md` e diagramas |
| 21 | Implementar no próprio diretório `PayFlow`, e não em outro | Trabalho movido do worktree separado para o repositório principal |
| 22 | Pular os testes e seguir para o documento (.docx) | Geração do documento de entrega (`docs/Entrega_VerticalSlice_SOLID.docx`) |

## Estado resultante

A base utiliza Java 21, Spring Boot, MongoDB Atlas e Docker. Na branch `feature/vertical-slice-solid`, Vertical Slice, Clean Architecture e SOLID estão aplicados a duas funcionalidades: provisionamento do lojista demo e autenticação por API Key (`GET /v1/merchants/me`). A `main` permanece na base anterior às refatorações.

PostgreSQL, Flyway e H2 foram substituídos. Pagamentos, portal e notificações continuam pendentes. Azure é o destino planejado; a implantação no Azure e a publicação da imagem não foram realizadas neste histórico.

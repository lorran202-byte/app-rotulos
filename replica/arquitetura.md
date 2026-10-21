# Arquitetura

- Kotlin 2.0.21, Jetpack Compose (BOM 2024.12.01, Material 3), AGP 8.7.3, Gradle 8.11.1
- minSdk 26, target/compileSdk 35, package `com.lorran.lupanutri`
- MVVM: um `AppViewModel` por tela-chave + repositório único (`ProductRepository`)
- Room (produtos em cache, histórico, favoritos, cadastros manuais) via KSP
- DataStore (objetivo e restrições do perfil)
- CameraX + ML Kit Barcode (modelo embutido, funciona offline)
- Rede: `HttpURLConnection` + `org.json` (sem bibliotecas extras) contra a API pública do Open Food Facts
- Coil para imagens

## Permissões
- `CAMERA` — ler código de barras
- `INTERNET` / `ACCESS_NETWORK_STATE` — consultar a base aberta de produtos

## Nota própria (0–100)
Começa em 100 e, por 100 g (ou 100 ml em bebidas):
- penaliza calorias, açúcares, gordura saturada, sódio, gordura trans, ultraprocessamento (NOVA 4), aditivos (mais para os de maior atenção) e adoçantes;
- bonifica fibras, proteína e alimento in natura (NOVA 1).
O objetivo do perfil muda os pesos (emagrecimento, ganho de massa, pressão, controle de açúcar, infantil).
Faixas: 0–24 Evite · 25–49 Ruim · 50–74 Bom · 75–100 Ótimo. Sem tabela nutricional = sem nota.
Alertas de lupa com os limites da RDC 429/2020 (sólidos: açúcar ≥ 15 g, gord. sat. ≥ 6 g, sódio ≥ 600 mg; líquidos: metade).

## Ordem de construção
1. Scanner → busca do produto → tela de produto com nota (fluxo principal)
2. Histórico e favoritos
3. Perfil (objetivo + restrições) refletindo na nota e nos alertas
4. Busca por nome, alternativas, cadastro manual, compartilhar

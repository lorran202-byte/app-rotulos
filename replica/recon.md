# Recon — Desrotulando (referência)

Fontes públicas: ficha da App Store (apps.apple.com/br/app/desrotulando/id1071285904), ficha da Google Play (com.desrotulando.app), matéria do Showmetech.

## O que o app faz
- Escaneia o código de barras de um alimento e mostra uma nota de 0 a 100.
- Tela do produto: nota, pontos positivos e negativos, composição (ingredientes e nutrientes).
- Busca por nome, marca ou categoria (já foi paga, hoje gratuita segundo o desenvolvedor).
- "Melhores alternativas": produtos mais bem avaliados da mesma categoria.
- Perfis de objetivo (perda de peso, infantil, hipertensão, diabetes) que mudam a nota.
- Restrições alimentares (16 categorias: glúten, leite, soja, açúcar, gordura trans, adoçantes, corantes…).
- Listas de compras e favoritos.
- Envio de rótulos de produtos que não estão na base (com IA lendo a embalagem).
- Conteúdo semanal de especialistas.
- Modelo: grátis com 3 produtos por dia; Premium/Clube pago para o resto.

## Fluxo principal
Abrir app → câmera → ler código de barras → ver nota e detalhes → (salvar / ver alternativas).

## Modelo de dados provável
Produto(código, nome, marca, categoria, imagem, ingredientes, nutrientes por 100 g, aditivos, alergênicos) · Perfil(objetivo, restrições) · Histórico · Favoritos · Lista de compras.

## Fora (conteúdo, não função)
- Base de produtos e notas do Desrotulando → usaremos o Open Food Facts (base aberta, licença ODbL).
- Algoritmo e textos dos nutricionistas deles → nota própria, baseada em critérios públicos (lupa da Anvisa RDC 429/2020, grau de processamento NOVA, aditivos, perfil de nutrientes).
- Conteúdo semanal de especialistas.

## Tamanho
Médio. V1 enxuta: scanner + nota + detalhe + histórico + favoritos + perfil/restrições + busca + cadastro manual. Alternativas por categoria entram se a API responder bem (should).

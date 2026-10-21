# Testes — v1.0

Feito:
- Compilação release no GitHub Actions: OK (12 MB, arm64-v8a + armeabi-v7a).
- APK conferido com aapt2/apksigner: package com.lorran.lupanutri, versão 1 (1.0), minSdk 26 / target 35,
  permissões só CAMERA, INTERNET e ACCESS_NETWORK_STATE, assinado com a keystore própria (keystore/).
- Testes de unidade da nota e das restrições: 5/5 passando (local e no CI).
- Lint: 0 erros, 16 avisos (versões de bibliotecas mais novas, pasta v26 redundante) — nenhum bloqueante.

Não deu para fazer neste ambiente:
- Rodar em emulador/aparelho (sem Android SDK aqui) — primeiro teste real é no celular.
- Chamar a API do Open Food Facts daqui (rede bloqueada) — formato seguido pela documentação pública.

Bugs abertos: nenhum S1/S2 conhecido.
Casos de borda tratados no código: sem internet (usa o que está salvo / mensagem com tentar de novo),
câmera negada (mensagem + abrir configurações + digitar código), produto não encontrado (cadastro manual),
produto sem tabela nutricional (sem nota + completar dados), listas vazias, voltar no meio do cadastro.

# Paridade
must 11/11 (33) · should 6/6 (12) · could 1/2 (1) → 46/47 = 98%. Falta: lista de compras (could).

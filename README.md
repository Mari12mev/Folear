# Folhear — Leitor de Livros e Quadrinhos para Android

Aplicativo Android nativo, responsivo e pensado para celular, desenvolvido em **Kotlin** e **Jetpack Compose** com persistência local em **Room Database (SQLite)**. O aplicativo funciona **100% offline**, sem dependência de internet ou servidores externos.

---

## 🚀 Recursos e Etapas Implementadas

### ETAPA 1: Leitor de HQs e Mangás (CBZ / CBR)
- **4 Modos de Leitura**:
  - `LTR` (Página única ocidental: esquerda → direita)
  - `RTL` (Mangá invertido: direita → esquerda, toque na direita volta, esquerda avança)
  - `VERTICAL_PAGED` (Vertical de página em página)
  - `WEBTOON` (Rolagem contínua vertical fluida sem cortes)
- **Salvamento automático do modo preferido** por livro.
- **Zonas de Toque Inteligentes**: 25% lateral esquerda/direita para virada rápida (com inversão automática no modo Mangá RTL) e 50% central para exibir/ocultar barras de controle.
- **Corte Automático de Bordas Brancas (Crop)**: Algoritmo de detecção e recorte inteligente de margens vazias.
- **Zoom Dinâmico**: Pinch-to-zoom (pinça) e Toque Duplo suave (alternando entre 1x e 2.5x).
- **Pré-carregamento em Segundo Plano**: Cache de memória LRU que pré-decodifica páginas vizinhas, eliminando travamentos ao folhear.

### ETAPA 2: Leitor de Livros (EPUB)
- **Personalização Tipográfica**:
  - 4 Famílias de fontes: Sans-Serif moderna, Serifada clássica, Monoespaçada e Cursiva elegante.
  - Ajuste de tamanho da fonte (slider de 12sp a 32sp).
  - Espaçamento entre linhas (1.2x a 2.0x).
  - Margens horizontais configuráveis (16dp, 28dp, 44dp).
  - Alinhamento de texto: Justificado ou Alinhado à esquerda.
- **4 Temas de Leitura Confortáveis**:
  - **Claro** (Papiro Creme `#FAF6F0`)
  - **Sépia** (Amadeirado acolhedor `#F4ECD8`)
  - **Escuro** (Espresso Noturno `#141211`)
  - **AMOLED** (Preto Puro `#000000`, máxima economia de bateria)
- **Brilho e Filtro Noturno**:
  - Controle de brilho da tela direto pelo leitor.
  - Filtro de luz azul quente regulável (0% a 50%).
- **Busca Interna no Livro**: Pesquisa rápida em todos os capítulos do EPUB com prévia contextual de ocorrências e salto direto para a posição encontrada.

### ETAPA 3: Marcadores, Destaques e Notas
- **Marcadores Rápidos**: Marque páginas ou capítulos com um toque no ícone de fita na barra superior.
- **Destaques Coloridos**: Destaque trechos de texto em EPUB escolhendo entre 6 cores (Amarelo, Rosa, Verde, Azul, Laranja, Roxo).
- **Notas Reflexivas**: Adicione comentários e anotações a qualquer trecho destacado.
- **Painel de Anotações**: Visualização centralizada de todos os marcadores e notas da obra.
- **Exportação em Markdown (.md)**: Compartilhe ou exporte todas as notas e citações em formato Markdown estruturado para Obsidian, Notion ou arquivo de texto.

### ETAPA 4: Estatísticas, Histórico e Metas
- **Estatísticas Automáticas**:
  - Tempo total de leitura acumulado (horas e minutos).
  - Total de páginas lidas.
  - Sequência de leitura (*streak* de dias consecutivos lendo).
- **Gráficos em Canvas**: Visualização em barras da distribuição de leitura por **Semana** (Seg a Dom), **Mês** e **Ano**.
- **Histórico Recente**: Lista dos últimos livros abertos com data/hora e atalho para retomar instantaneamente.
- **Alimentação Automática das Metas**: Ao ler e avançar páginas, o progresso das metas ativas é atualizado automaticamente.

### ETAPA 5: Organização Avançada (Tags, Pastas e Filtros)
- **Pastas Personalizadas**: Crie coleções com ícones e cores customizados. Cada item pode estar em várias pastas simultaneamente.
- **Tags Personalizadas**: Crie tags temáticas (Ficção, Ação, Mangá, etc.) e filtre a biblioteca rapidamente.
- **Filtros de Leitura**: *Todos*, *Não lidos* (0%), *Em andamento* (1-99%), *Concluídos* (100%) e *★ Favoritos*.
- **Ordenação Dinâmica**:
  - Por Última Leitura
  - Por Título (A-Z)
  - Por Autor
  - Por Data de Adição
  - Por Progresso (% lido)
- **Edição Completa**: Edite o título e autor de qualquer item importado e altere o status de favorito com 1 toque.

### ETAPA 6: Backup, Leitura em Voz Alta (TTS) e Modo Incógnito
- **Backup e Restauração (.json)**:
  - Exporte todo o banco (livros, pastas, tags, progresso, metas, marcadores e notas) em um único arquivo JSON seguro.
  - Restaure a qualquer momento usando o seletor de arquivos do Android.
- **Leitura em Voz Alta (TTS)**:
  - Motor nativo do sistema (`android.speech.tts.TextToSpeech`).
  - Barra de controle flutuante com Play/Pausa, anterior, próximo e velocidades reguláveis (0.75x, 1.0x, 1.25x, 1.5x, 2.0x).
- **Modo Incógnito**:
  - Interruptor no cabeçalho e leitor. Quando ativo, exibe badge discreto e suspende a gravação de histórico de sessão e estatísticas.

---

## 📲 Como Gerar e Instalar o APK Atualizado Sem Perder Dados

### 1. Geração do APK Debug
Para gerar o arquivo APK de depuração, o Gradle compila o projeto com a chave de debug estável do repositório:
```bash
gradle assembleDebug
```
O arquivo APK resultante será gerado em:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 2. Instalação por cima sem perda de dados
Como o identificador de aplicativo (`applicationId = "com.aistudio.folhear.bkqz"`) e a assinatura de debug (`debug.keystore`) permanecem inalterados, o Android reconhece a instalação como uma **atualização no mesmo app**.

O banco de dados Room (SQLite) e os arquivos em `filesDir` são **preservados integralmente** pelo sistema operacional Android.

#### Via ADB (linha de comando):
Utilize o parâmetro `-r` (*reinstall keeping its data*):
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

#### No dispositivo Android:
1. Transfira o arquivo `app-debug.apk` para o celular (ou baixe via gerenciador de arquivos).
2. Abra o arquivo APK. O instalador do pacote do Android exibirá:
   > *"Deseja instalar uma atualização para este app? Seus dados existentes não serão perdidos."*
3. Toque em **Atualizar**.
4. Todos os seus livros, pastas, progresso, metas e anotações continuarão exatamente onde você os deixou!

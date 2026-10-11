# Folhear — Expansão Completa do Leitor de Livros e Quadrinhos

Plano arquitetural e de execução das 6 etapas de evolução do leitor de livros e HQs, preservando 100% da base existente (Kotlin, Jetpack Compose e Room Database no Android), com suporte offline absoluto e execução incremental com verificação de build a cada etapa.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> **Base Tecnológica Existente**: O projeto já opera com arquitetura nativa Android (Kotlin + Jetpack Compose + Room SQLite), garantindo máxima fluidez de 120 FPS em renderização de PDF, descompressão CBZ e Webtoon contínuo. Todas as novas entidades e preferências serão integradas ao Room mantendo compatibilidade e migração transparente.

- **Modos de HQ/Mangá**: Integração de 4 modos selecionáveis e lembrados por livro: Página Única LTR (ocidental), Página Única RTL (mangá invertido), Vertical Paginado e Webtoon Contínuo com pré-carregamento assíncrono.
- **Armazenamento de Anotações e Estatísticas**: Novas tabelas Room (`bookmarks`, `annotations`, `reading_sessions`, `tags`) com exportação unificada em arquivo JSON via seletor nativo do Android.
- **Text-to-Speech (TTS)**: Uso direto da API nativa do sistema (`android.speech.tts.TextToSpeech`), garantindo zero dependências externas ou de rede.

---

## 1. Visão Geral da Arquitetura e Fluxo

```
┌────────────────────────────────────────────────────────────────────────┐
│                        INTERFACE JETPACK COMPOSE                       │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────┐  │
│  │ Biblioteca &     │  │ Pastas, Tags &   │  │ Metas & Estatísticas │  │
│  │ Filtros Avançados│  │ Organização      │  │ com Gráficos Canvas  │  │
│  └────────┬─────────┘  └────────┬─────────┘  └──────────┬───────────┘  │
│           │                     │                       │              │
│  ┌────────▼─────────────────────▼───────────────────────▼───────────┐  │
│  │                        LEITOR IMERSIVO                           │  │
│  │  ┌─────────────────────────────┐ ┌─────────────────────────────┐ │  │
│  │  │   Leitor HQs/Mangás/Webtoon │ │   Leitor EPUB & TTS         │ │  │
│  │  │  • LTR / RTL / Vertical     │ │  • Temas (Claro/Escuro/OLED)│ │  │
│  │  │  • Corte de Bordas Brancas  │ │  • Fontes, Margens, Busca   │ │  │
│  │  │  • Double-tap Zoom & Cache  │ │  • Destaques, Notas & Voz   │ │  │
│  │  └─────────────────────────────┘ └─────────────────────────────┘ │  │
│  └──────────────────────────────────┬───────────────────────────────┘  │
└─────────────────────────────────────┼──────────────────────────────────┘
                                      │
┌─────────────────────────────────────▼──────────────────────────────────┐
│                           CAMADA DE DADOS                              │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                        Room Database (SQLite)                    │  │
│  │  • books (com modo preferido, favoritos e status)                │  │
│  │  • folders & book_folder_cross_ref                               │  │
│  │  • tags & book_tag_cross_ref                                     │  │
│  │  • reading_goals & reading_sessions (tempo, páginas, streaks)    │  │
│  │  • bookmarks & annotations (destaques com cores e notas)         │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                 Sistema de Arquivos & Backup                     │  │
│  │  • context.filesDir (Livros CBZ/PDF/EPUB e Capas)                │  │
│  │  • Exportador/Importador JSON (SAF Storage Access Framework)     │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Detalhamento das 6 Etapas de Implementação

### ETAPA 1: Leitor de HQs e Mangás Avançado
1. **Modos de leitura**:
   - `LTR` (Ocidental: esquerda → direita)
   - `RTL` (Mangá: direita → esquerda, toque invertido)
   - `VERTICAL_PAGED` (Vertical de página em página)
   - `WEBTOON` (Rolagem contínua fluida via `LazyColumn` com `Item` por página)
2. **Preferência salva por livro**: Persistência do modo no banco de dados (`comicReadingMode`).
3. **Zonas de toque**: 
   - 25% esquerda e direita para virada de página (adaptativa conforme o modo LTR/RTL).
   - 50% central para exibir/ocultar barras e controles.
4. **Corte automático de bordas brancas (Crop Margin)**: Algoritmo bitmap que detecta linhas e colunas uniformes nas extremidades e recorta a imagem para maximizar a área útil da tela.
5. **Zoom e Interação**: Pinch-to-zoom e Double-tap com animação suave para alternar entre 1x e 2.5x.
6. **Pré-carregamento inteligente**: Coroutine de segundo plano que pré-decodifica as páginas `N-1`, `N+1` e `N+2` em memória cache LRU, eliminando telas pretas ao folhear.

---

### ETAPA 2: Leitor de Livros (EPUB) com Tipografia e Busca
1. **Personalização tipográfica completa**:
   - 4 opções de família tipográfica: *Serifada Clássica*, *Sans-Serif Moderna*, *Monoespaçada*, *Elegante Cursiva*.
   - Tamanho da fonte regulável (12sp a 32sp).
   - Espaçamento entre linhas (1.2x, 1.5x, 1.8x, 2.0x).
   - Margens horizontais (Pequena, Média, Ampla).
   - Alinhamento de texto (Justificado vs Alinhado à esquerda).
2. **4 Temas de leitura**: Claro (Papiro Creme), Escuro (Espresso aconchegante), Sépia e AMOLED (Preto puro `#000000`).
3. **Ajuste de brilho por gesto lateral**: Deslize vertical na borda esquerda da tela alterando o brilho da janela com indicador visual.
4. **Filtro de luz azul**: Camada de temperatura de cor quente ativável para conforto noturno.
5. **Busca interna de texto no livro**: Barra de pesquisa que localiza termos no EPUB, exibindo lista de ocorrências com prévia contextual do parágrafo e salto direto para o capítulo.

---

### ETAPA 3: Marcadores, Destaques e Notas
1. **Entidades Room**:
   - `BookmarkEntity`: ID, bookId, página/capítulo, título e timestamp.
   - `AnnotationEntity`: ID, bookId, capítulo/página, texto destacado, nota pessoal, cor do marcador (Amarelo, Rosa, Verde, Azul, Laranja) e data.
2. **Interface de leitura**:
   - Botão rápido de marcar página na barra superior com feedback animado.
   - Menu contextual de seleção para destacar texto com paleta de cores e campo de anotação.
3. **Painel de Destaques e Marcadores**:
   - Aba lateral/modal reunindo todos os marcadores e anotações da obra com data e busca interna.
   - Opção de exportação para compartilhamento em **Texto puro (.txt)** ou **Markdown (.md)** formatado.

---

### ETAPA 4: Estatísticas, Histórico e Integração com Metas
1. **Registro automático**:
   - Entidade `ReadingSessionEntity`: tempo decorrido de leitura, páginas lidas no dia, livro e data.
   - Contador de ofensiva (*streak* de leitura diária contínua e recorde pessoal).
2. **Tela de Estatísticas**:
   - Gráficos nativos em Canvas Jetpack Compose para visualização semanal (seg-dom), mensal e anual.
   - Métricas principais: tempo total lido, média diária e total de páginas devoradas.
3. **Histórico de leitura**:
   - Carrossel e lista dos últimos livros acessados ordenados por data e hora.
4. **Alimentação automática das metas**:
   - Cada página lida ou tempo registrado atualiza automaticamente as metas ativas correspondentes, sincronizando o status de ritmo ("No ritmo certo" / "Atrasado").

---

### ETAPA 5: Organização Avançada com Tags e Ordenação
1. **Tags personalizadas**:
   - Entidade `TagEntity` e tabela de relacionamento `BookTagCrossRef`.
   - Adição e remoção de tags por livro e filtro rápido por tags na biblioteca.
2. **Ordenação dinâmica da biblioteca**:
   - Por Título (A-Z)
   - Por Autor
   - Por Data de Adição
   - Por Última Leitura
   - Por Progresso (% lido)
3. **Filtros por estado de leitura**:
   - *Todos*, *Não lidos* (0%), *Em andamento* (1-99%), *Concluídos* (100%) e *Favoritos*.
4. **Ações rápidas**:
   - Marcar como favorito (coração) diretamente no card.
   - Marcar/desmarcar como concluído.
   - Diálogo para editar título e autor de qualquer item importado.

---

### ETAPA 6: Backup, TTS e Modo Incógnito
1. **Backup completo em arquivo único**:
   - Exportação de todo o banco (livros, pastas, tags, progresso, metas, destaques e histórico) em formato JSON.
   - Importação com restauração segura via Storage Access Framework (`CreateDocument` / `OpenDocument`).
2. **Leitura em voz alta (TTS)**:
   - Motor nativo `android.speech.tts.TextToSpeech` com controle de velocidade (0.75x a 2.0x), play, pausa e próximo parágrafo.
3. **Modo Incógnito**:
   - Interruptor rápido que suspende a gravação de histórico de sessão, estatísticas e alteração da data da última leitura.
4. **Documentação e Guia de Atualização do APK**:
   - Atualização do `README.md` detalhando todas as funções.
   - Instruções de compilação e instalação via `adb install -r` mantendo o banco Room intacto.

package com.example.sqlitecompose

// Importações básicas para Android e Jetpack Compose
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

// MainActivity principal da aplicação
class MainActivity : ComponentActivity() {

    // Data class que representa uma nota
    data class Note(
        val id: Long? = null,  // ID opcional (null se nota nova)
        val title: String,     // título da nota
        val content: String,   // conteúdo da nota
        val coment: String     // comentário da nota
    )

    // Classe helper para gerenciamento do banco SQLite dentro da MainActivity
    class DBHelper(context: Context) :
        SQLiteOpenHelper(context, "app.db", null, 2) { // banco app.db versão 2

        // Criação da tabela "notes" com colunas id, title, content, coment
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE notes(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    content TEXT NOT NULL,
                    coment TEXT
                )
                """.trimIndent()
            )
        }

        // Atualização do banco: remove a tabela antiga e recria
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS notes")
            onCreate(db)
        }

        // Insere uma nova nota no banco e retorna o id inserido
        fun insertNote(note: Note): Long {
            val cv = ContentValues().apply {
                put("title", note.title)
                put("content", note.content)
                put("coment", note.coment)
            }
            return writableDatabase.insert("notes", null, cv)
        }

        // Atualiza uma nota existente pelo id informado
        fun updateNote(note: Note): Int {
            requireNotNull(note.id) { "ID não pode ser nulo para update" }
            val cv = ContentValues().apply {
                put("title", note.title)
                put("content", note.content)
                put("coment", note.coment)
            }
            return writableDatabase.update(
                "notes",
                cv,
                "id=?",
                arrayOf(note.id.toString())
            )
        }

        // Remove a nota com o id informado do banco
        fun deleteNote(id: Long): Int {
            return writableDatabase.delete(
                "notes",
                "id=?",
                arrayOf(id.toString())
            )
        }

        // Retorna uma lista com todas as notas, ordenadas por id descrescente
        fun getAllNotes(): List<Note> {
            val list = mutableListOf<Note>()
            val c: Cursor = readableDatabase.rawQuery(
                "SELECT id, title, content, coment FROM notes ORDER BY id DESC",
                null
            )
            c.use { cur ->
                val idIdx = cur.getColumnIndexOrThrow("id")
                val titleIdx = cur.getColumnIndexOrThrow("title")
                val contentIdx = cur.getColumnIndexOrThrow("content")
                val comentIdx = cur.getColumnIndexOrThrow("coment")
                while (cur.moveToNext()) {
                    list.add(
                        Note(
                            id = cur.getLong(idIdx),
                            title = cur.getString(titleIdx),
                            content = cur.getString(contentIdx),
                            coment = cur.getString(comentIdx)
                        )
                    )
                }
            }
            return list
        }
    }

    // Função executada ao criar a MainActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = DBHelper(this) // cria instância do helper do banco

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    NotesScreen(dbHelper = db) // inicia tela principal do app
                }
            }
        }
    }

    // Tela Compose com listagem e formulário para notas (CRUD)
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun NotesScreen(dbHelper: DBHelper) {
        // Estado que guarda todas as notas atuais
        var notes by remember { mutableStateOf(dbHelper.getAllNotes()) }

        // Estados para campos do formulário (usando TextFieldValue)
        var title by remember { mutableStateOf(TextFieldValue("")) }
        var content by remember { mutableStateOf(TextFieldValue("")) }
        var coment by remember { mutableStateOf(TextFieldValue("")) }

        // Guarda o id atualmente em edição (null se nova nota)
        var editingId by remember { mutableStateOf<Long?>(null) }

        // Função para limpar campos e resetar edição
        fun clearFields() {
            title = TextFieldValue("")
            content = TextFieldValue("")
            coment = TextFieldValue("")
            editingId = null
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (editingId == null)
                                "Notas (SQLite + Compose) João Xavier"
                            else "Editando #$editingId"
                        )
                    }
                )
            }
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                // Campo texto para título
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                // Campo texto para conteúdo
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Conteúdo") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                // Campo texto para comentário
                OutlinedTextField(
                    value = coment,
                    onValueChange = { coment = it },
                    label = { Text("Comentário") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                // Linha com botões Salvar/Atualizar e Limpar
                Row {
                    Button(
                        onClick = {
                            // Remove espaços desnecessários dos textos
                            val t = title.text.trim()
                            val c = content.text.trim()
                            val cm = coment.text.trim()
                            // Impede salvar se algum dos campos estiver vazio
                            if (t.isEmpty() || c.isEmpty() || cm.isEmpty()) return@Button

                            if (editingId == null) {
                                // Cria nova nota no banco
                                dbHelper.insertNote(Note(title = t, content = c, coment = cm))
                            } else {
                                // Atualiza nota existente pelo id
                                dbHelper.updateNote(
                                    Note(id = editingId, title = t, content = c, coment = cm)
                                )
                            }
                            // Atualiza a lista de notas na UI
                            notes = dbHelper.getAllNotes()
                            // Limpa campos após salvar/atualizar
                            clearFields()
                        }
                    ) {
                        Text(if (editingId == null) "Salvar" else "Atualizar")
                    }
                    Spacer(Modifier.width(8.dp))
                    // Botão para limpar campos manualmente
                    OutlinedButton(onClick = { clearFields() }) {
                        Text("Limpar")
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                Spacer(Modifier.height(8.dp))

                // Lista de notas em LazyColumn para rolagem
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(notes, key = { it.id ?: -1 }) { note ->
                        NoteItem(
                            note = note,
                            onClick = {
                                // Carrega dados da nota para edição quando clicado
                                editingId = note.id
                                title = TextFieldValue(note.title)
                                content = TextFieldValue(note.content)
                                coment = TextFieldValue(note.coment)
                            },
                            onDelete = { id ->
                                // Exclui nota do banco e atualiza lista
                                dbHelper.deleteNote(id)
                                notes = dbHelper.getAllNotes()
                                // Limpa campos se nota excluída estava em edição
                                if (editingId == id) clearFields()
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }

    // Composable para exibir cada item individual da lista
    @Composable
    private fun NoteItem(
        note: Note,
        onClick: () -> Unit,
        onDelete: (Long) -> Unit
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { onClick() } // ao clicar, ativa edição
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // Exibe título da nota
                Text(text = note.title, style = MaterialTheme.typography.titleMedium)
                // Botão Excluir visível só se id existir
                if (note.id != null) {
                    TextButton(onClick = { onDelete(note.id) }) {
                        Text("Excluir")
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            // Exibe conteúdo da nota
            Text(text = note.content, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            // Exibe comentário da nota
            Text(text = note.coment, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

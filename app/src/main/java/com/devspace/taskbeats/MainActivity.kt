package com.devspace.taskbeats

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private var categories = listOf<CategoryUiData>();
    private var categoriesEntity = listOf<CategoryEntity>();
    private var tasks = listOf<TaskUiData>();

    private lateinit var rvCategory: RecyclerView
    private lateinit var ctnEmptyView: LinearLayout
    private lateinit var fabCreateTask: FloatingActionButton

    private val categoryAdapter = CategoryListAdapter();
    private val taskAdapter by lazy {
        TaskListAdapter();
    }

    private val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            TaskBeatDatabase::class.java, "database-task-beat"
        ).build();
    }

    private val categoryDao: CategoryDao by lazy {
        database.getCategoryDao();
    }

    private val taskDao: TaskDao by lazy {
        database.getTaskDao();
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvCategory = findViewById(R.id.rv_categories);
        ctnEmptyView = findViewById(R.id.ll_empty_view);
        val rvTask = findViewById<RecyclerView>(R.id.rv_tasks);
        fabCreateTask = findViewById(R.id.fab_create_task);
        val btnCreateEmpty = findViewById<Button>(R.id.btn_create_empty);

        btnCreateEmpty.setOnClickListener {
            showCreateCategoryBottomSheet();
        }

        taskAdapter.setOnClickListenner { task ->
            showCreateUpdateTaskBottomSheet(task);
        }

        categoryAdapter.setOnLongClickListener { categoryToBeDeleted ->
            if(categoryToBeDeleted.name != "+" && categoryToBeDeleted.name != "ALL") {
                val title: String = this.getString(R.string.category_delete_title);
                val description = this.getString(R.string.category_delete_description);
                val btnText = this.getString(R.string.delete);


                showInfoDialog(title, description, btnText) {
                    val categoryEntityToBeDeleted = CategoryEntity(categoryToBeDeleted.name, categoryToBeDeleted.isSelected);
                    deleteCategory(categoryEntityToBeDeleted);
                }
            }
        }

        categoryAdapter.setOnClickListener { selected ->

            if(selected.name == "+") {
                showCreateCategoryBottomSheet();
            }
            else {
                val categoryTemp = categories.map { item ->
                    when {
                        item.name == selected.name && item.isSelected -> item.copy(isSelected = true)
                        item.name == selected.name && !item.isSelected -> item.copy(isSelected = true)
                        item.name != selected.name && item.isSelected -> item.copy(isSelected = false)
                        else -> item
                    }
                }

                    if (selected.name != "ALL") {
                        filterTaskByCategoryName(selected.name);
                    } else {
                        getTasksFromDatabase();
                    }

                categoryAdapter.submitList(categoryTemp)
            }
        }

        rvCategory.adapter = categoryAdapter
        getCategoriesFromDatabase()

        rvTask.adapter = taskAdapter
        getTasksFromDatabase();

        fabCreateTask.setOnClickListener {
            showCreateUpdateTaskBottomSheet();
        }

    }

    private fun showInfoDialog(title: String, description: String, btnText: String, onClick: () -> Unit) {
        val infoBottomSheet = InfoBottomSheet(
            title = title,
            description = description,
            btnText = btnText,
            onClicked = onClick
        );

        infoBottomSheet.show(supportFragmentManager, "infoBottomSheet")
    }

    private fun getCategoriesFromDatabase(){
        GlobalScope.launch(Dispatchers.IO) {
            val categoriesFromDb: List<CategoryEntity> = categoryDao.getAll()
            categoriesEntity = categoriesFromDb;

            if(categoriesEntity.isEmpty()) {
                withContext(Dispatchers.Main) {
                    rvCategory.isVisible = false;
                    ctnEmptyView.isVisible = true;
                    fabCreateTask.isVisible = false;
                }
            }
            else {
                withContext(Dispatchers.Main) {
                    rvCategory.isVisible = true;
                    ctnEmptyView.isVisible = false;
                    fabCreateTask.isVisible = true;
                }
            }

            val categoriesUiData = categoriesFromDb.map { category ->
                CategoryUiData(category.name, category.isSelected)
            }.toMutableList();


            categoriesUiData.add(
                CategoryUiData("+", false)
            )

            val categoryListTemp = mutableListOf(CategoryUiData(
                name = "ALL",
                isSelected = true,
            ));

            categoryListTemp.addAll(categoriesUiData);
            categories = categoryListTemp;

            withContext(Dispatchers.Main) {
                categoryAdapter.submitList(categories);
            }
        }
    }

    private fun getTasksFromDatabase() {
        GlobalScope.launch(Dispatchers.IO) {
            val tasksFromDb: List<TaskEntity> = taskDao.getAll()

            val tasksUiData = tasksFromDb.map { task ->
                TaskUiData(task.id, task.name,task.category);
            }

            tasks = tasksUiData;

            withContext(Dispatchers.Main) {
                taskAdapter.submitList(tasksUiData);
            }
        }
    }

    private fun insertCategory(categoryEntity: CategoryEntity) {
        GlobalScope.launch(Dispatchers.IO) {
            categoryDao.insert(categoryEntity);
            getCategoriesFromDatabase();
        }
    }

    private fun insertTask(taskEntity: TaskEntity) {
        GlobalScope.launch(Dispatchers.IO) {
            taskDao.insert(taskEntity);
            getTasksFromDatabase();
        }
    }

    private fun updateTask(taskEntity: TaskEntity) {
        GlobalScope.launch(Dispatchers.IO) {
            taskDao.update(taskEntity);
            getTasksFromDatabase();
        }
    }

    private fun deleteTask(taskEntity: TaskEntity) {
        GlobalScope.launch(Dispatchers.IO) {
            taskDao.delete(taskEntity);
            getTasksFromDatabase();
        }
    }

    private fun deleteCategory(categoryEntity: CategoryEntity) {
        GlobalScope.launch(Dispatchers.IO) {
            val tasksToBeDeleted = taskDao.getAllByCategoryName(categoryEntity.name);
            taskDao.deleteAll(tasksToBeDeleted);
            categoryDao.delete(categoryEntity);
            getCategoriesFromDatabase();
            getTasksFromDatabase();
        }
    }

    private fun filterTaskByCategoryName(category: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val tasksFromDb: List<TaskEntity> = taskDao.getAllByCategoryName(category);
            val tasksUiData: List<TaskUiData> = tasksFromDb.map {
                TaskUiData(
                    id = it.id,
                    name = it.name,
                    category = it.category
                )
            }

            withContext(Dispatchers.Main) {
                taskAdapter.submitList(tasksUiData);
            }
        }
    }

    private fun showCreateUpdateTaskBottomSheet(taskUiData: TaskUiData? = null) {
        val createOrUpdateTaskBottomSheet = CreateOrUpdateTaskBottomSheet(
            categoriesEntity,
            taskUiData,
            onCreateClicked = { taskToBeCreated ->
                val taskEntityToBeInsert = TaskEntity(name = taskToBeCreated.name, category = taskToBeCreated.category)
                insertTask(taskEntityToBeInsert);
            },
            onUpdateClicked = { taskToBeUpdated ->
                val taskEntityToBeUpdate = TaskEntity(id = taskToBeUpdated.id, name = taskToBeUpdated.name, category = taskToBeUpdated.category);
                updateTask(taskEntityToBeUpdate);
            },
            onDeleteClicked = { taskToBeDeleted ->
                val taskEntityToBeDelete = TaskEntity(id = taskToBeDeleted.id, name = taskToBeDeleted.name, category = taskToBeDeleted.category);
                deleteTask(taskEntityToBeDelete);
            }
        );

        createOrUpdateTaskBottomSheet.show(supportFragmentManager, "createTaskBottomSheet");
    }

    private fun showCreateCategoryBottomSheet() {
        val createCategoryBottomSheet = CreateCategoryBottomSheet { categoryName ->
            val categoryEntity = CategoryEntity(name = categoryName, isSelected = false);

            insertCategory(categoryEntity);
        };
        createCategoryBottomSheet.show(supportFragmentManager, "createCategoryBottomSheet");
    }

}
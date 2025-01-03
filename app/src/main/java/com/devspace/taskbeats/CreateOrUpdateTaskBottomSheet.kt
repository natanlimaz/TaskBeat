package com.devspace.taskbeats

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

class CreateOrUpdateTaskBottomSheet(
    private val categories: List<CategoryEntity>,
    private val task: TaskUiData? = null,
    private val onCreateClicked: (TaskUiData) -> Unit,
    private val onUpdateClicked: (TaskUiData) -> Unit,
    private val onDeleteClicked: (TaskUiData) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.create_or_update_task_bottom_sheet, container, false);

        val tvTitle = view.findViewById<TextView>(R.id.tv_title);
        val btnCreateOrUpdate = view.findViewById<Button>(R.id.btn_task_create);
        val btnDelete = view.findViewById<Button>(R.id.btn_task_delete);
        val tieTaskName = view.findViewById<TextInputEditText>(R.id.tie_task_name);
        val spinner = view.findViewById<Spinner>(R.id.category_list);

        var taskCategory : String? = null;
        val categoryListTemp = mutableListOf<String>("Select")
        categoryListTemp.addAll(
            categories.map { it.name }
        )
        val categoriesStr: List<String> = categoryListTemp;

        ArrayAdapter(
            requireActivity().baseContext,
            android.R.layout.simple_spinner_item,
            categoriesStr
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                taskCategory = categoriesStr[position];
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

        }

        if(task == null) {
            tvTitle.setText(R.string.create_task_title);
            btnCreateOrUpdate.setText(R.string.create)
            btnDelete.isVisible = false;
        }
        else {
            tvTitle.setText(R.string.update_task_title);
            btnCreateOrUpdate.setText(R.string.update);
            tieTaskName.setText(task.name);
            btnDelete.isVisible = true;

            val currentCategory = categories.first { it.name == task.category };
            val index = categories.indexOf(currentCategory);

            spinner.setSelection(index);
        }

        btnDelete.setOnClickListener {
            if(task != null) {
                onDeleteClicked.invoke(task);
                dismiss();
            }
            else {
                Log.d("CreateOrUpdateTaskBottomSheet", "Task not found");
            }

        }

        btnCreateOrUpdate.setOnClickListener {
            val name = tieTaskName.text.toString().trim();
            if(taskCategory != "Select" && name.isNotEmpty()) {

                if(task == null) {
                    onCreateClicked.invoke(
                        TaskUiData(
                            id = 0,
                            name = name,
                            category =  requireNotNull(taskCategory)
                        )
                    );
                }
                else {
                    onUpdateClicked.invoke(
                        TaskUiData(
                            id = task.id,
                            name = name,
                            category = requireNotNull(taskCategory)
                        )
                    );
                }


                dismiss();
            }
            else {
                Snackbar.make(btnCreateOrUpdate, "Please select a category", Snackbar.LENGTH_LONG).show();
            }
        }

        return view;
    }
}
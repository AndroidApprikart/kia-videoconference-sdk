package com.kia.vc.feedback

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.vc.AndroidUtils
import com.app.vc.R
import com.app.vc.databinding.ActivityFeedbackBinding
import com.app.vc.feedback.SurveyData
import com.google.android.material.button.MaterialButton


private const val TAG = "FeedbackActivity:"

class FeedbackActivity : AppCompatActivity() {
    lateinit var feedBackViewModel:FeedbackViewModel
    lateinit var binding : ActivityFeedbackBinding

    lateinit var layoutManager: LinearLayoutManager
    lateinit var feedbackQuestionAdapter: FeedbackQuestionAdapter
    var questionList  = ArrayList<ModifiedSurveyData>()

    val commentsMap: HashMap<Int, String> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate: FeedbackActivity: ")
        setContentView(R.layout.activity_feedback)
        init()
        getIntentValues()

        initializeObservers()
        initializeFeedbackListAdapter()


        initializeApiResponseObservers()
        feedBackViewModel.isProgressBarVisible.value = true

        if(AndroidUtils.isNetworkAvailable(this)) {
            feedBackViewModel.isProgressBarVisible.value = true
//            newImplementation_20Sep2023
//            feedBackViewModel.getSurveyQuestionList()
            feedBackViewModel.getSurveyQuestionListNew()
        }else {
            feedBackViewModel.isProgressBarVisible.value = false
            binding.cardApiFailure.visibility = View.VISIBLE
            feedBackViewModel.toastString.value = "No Internet Available"
        }

        
        initializeRatingListener()
        initializeOnclickListener()

    }


    private fun getIntentValues() {
        feedBackViewModel.customerCode = intent.getStringExtra("customerCode")
        feedBackViewModel.dealerCode = intent.getStringExtra("dealerCode")
        feedBackViewModel.roNo = intent.getStringExtra("roNumber")
        feedBackViewModel.meetingCode = intent.getStringExtra("meetingCode")
        feedBackViewModel.userName = intent.getStringExtra("userName")
    }
    private fun initializeOnclickListener() {
        binding.btnSubmitFeedBack.setOnClickListener {
            if(AndroidUtils.isNetworkAvailable(this)) {
                if(feedbackQuestionAdapter.dataList.isNotEmpty()) {
                    if(!checkIfRatingIsNotProvided(feedbackQuestionAdapter.dataList)) {
                        if(!checkIfCommentsAreEmpty(feedbackQuestionAdapter.dataList)) {
                            Log.d(TAG, "initializeOnclickListener: dataList: ${feedbackQuestionAdapter.dataList}")
//                            newImplementation_20Sep2023
//                            feedBackViewModel.postSurveyDetails(getCreateSurveyRequestObject())
                            feedBackViewModel.postSurveyDetailsNew(getCreateSurveyRequestObject())
                        }else {
                            feedBackViewModel.toastString.value = "Comments cannot be empty"
                        }
                    }else {
                        feedBackViewModel.toastString.value = "Rating cannot be left empty"
                    }
                }else {
                    feedBackViewModel.toastString.value = "Something went wrong. SubmitFeedback"
                }
            }else {
                feedBackViewModel.toastString.value = "No Internet connection."
            }
        }

        binding.btnCancelFeedBack.setOnClickListener {
            finish()
        }
        
        binding.btnGoBack.setOnClickListener {
            finish()
        }
    }


    private fun checkIfRatingIsNotProvided(surveyData: ArrayList<ModifiedSurveyData>):Boolean {
        for(surveyDataItem in surveyData) {
            if(surveyDataItem.rating ==0) {
                return true
            }
        }

        return false
    }

    private fun checkIfCommentsAreEmpty(surveyData: ArrayList<ModifiedSurveyData>):Boolean {
        for(surveyDataItem in surveyData) {
            Log.d(TAG, "checkIfCommentsAreEmpty: ratingtest: ${surveyDataItem.rating}")
            Log.d(TAG, "checkIfCommentsAreEmpty: ratingtest: ${surveyDataItem.comment}")
            if(surveyDataItem.rating<=3 && surveyDataItem.comment.trim().isEmpty()) {
                return true
            }
        }
        return false
    }

    private fun getCreateSurveyRequestObject():RequestModelCreateSurvey {
        if(!feedBackViewModel.roNo.isNullOrEmpty()) {
            return RequestModelCreateSurvey(
                customerCode =feedBackViewModel.customerCode.toString() ,
                dealerNumber =feedBackViewModel.dealerCode.toString() ,
                roNumber = feedBackViewModel.roNo.toString(),
                meetingCode = feedBackViewModel.meetingCode.toString(),
                feedback = getFeedbackList(),
                userName = feedBackViewModel.userName.toString() ,
            )
        }
        return RequestModelCreateSurvey(
            customerCode =feedBackViewModel.customerCode.toString() ,
            dealerNumber =feedBackViewModel.dealerCode.toString() ,
            roNumber = "",
            meetingCode = feedBackViewModel.meetingCode.toString(),
            feedback = getFeedbackList(),
            userName = feedBackViewModel.userName.toString() ,
        )


    }

    private fun getFeedbackList(): ArrayList<FeedbackListModel> {
        var feedback = ArrayList<FeedbackListModel>()

        for(i in feedbackQuestionAdapter.dataList) {
            feedback.add(
                FeedbackListModel(
                    comments = i.comment,
                    rating = i.rating.toString(),
                    cmm_code = i.surveyData.cmmCode
                )
            )
        }

        return feedback
    }

    private fun initializeRatingListener() {
        feedbackQuestionAdapter.commentDialogListener = object : FeedbackQuestionAdapter.CommentDialogListener {
            override fun onCommentDialogRequested(question: String, position: Int,rating:Double) {
                Log.d(TAG, "onCommentDialogRequested: ${position}:: ${question}")

                showCommentDialog(
                    question = question,
                    position = position,
                    rating = rating
                )
            }

        }
    }
    private fun init() {
        feedBackViewModel = FeedbackViewModel()
        binding = DataBindingUtil.setContentView(this,R.layout.activity_feedback)
        binding.feedbackVM = feedBackViewModel
    }

    private fun initializeObservers() {
        feedBackViewModel.toastString.observe(this) {
            Toast.makeText(this,it,Toast.LENGTH_SHORT).show()
        }

        feedBackViewModel.isProgressBarVisible.observe(this) {
            if(it) {
                binding.pbFeedBack.visibility = View.VISIBLE
            }else {
                binding.pbFeedBack.visibility = View.GONE
            }
        }

        feedBackViewModel.isFailureMessageVisible.observe(this) {
            if(it) {
                binding.cardApiFailure.visibility = View.VISIBLE
            }else {
                binding.cardApiFailure.visibility - View.GONE
            }
        }

        feedBackViewModel.isRatingListVisible.observe(this) {
            if(it) {
                binding.rlRateUsLayout.visibility = View.VISIBLE
            }else {
                binding.rlRateUsLayout.visibility = View.GONE
            }
        }
    }

    private fun initializeApiResponseObservers() {
        feedBackViewModel.mSurveyQuestionListResponse.observe(this) {
            Log.d(TAG, "initializeApiResponseObservers: ${it}")
            if(it!=null) {
                if(it.surveyQuestionListData.surveyList.isNotEmpty()) {
                    Log.d(TAG, "initializeApiResponseObservers: surveyList: Not empty")
                    renderQuestionList(it.surveyQuestionListData.surveyList as ArrayList<SurveyData>)
                }else {
                    feedBackViewModel.isFailureMessageVisible.value = true
                    feedBackViewModel.isProgressBarVisible.value = false
                    Log.d(TAG, "initializeApiResponseObservers: questionList : Empty")
                    feedBackViewModel.toastString.value = "Question List is Empty.SurveyQuestionList"
                }
            }else {
                feedBackViewModel.isFailureMessageVisible.value = true
                feedBackViewModel.isProgressBarVisible.value = false
                Log.d(TAG, "initializeApiResponseObservers: null response")
                feedBackViewModel.toastString.value = "Something went wrong.NullResponse.SurveyQuestionList."
            }
        }

        feedBackViewModel.postSurveyListResponse.observe(this) {
            if(it!=null) {
                if(it.status=="I") {
//                    feedBackViewModel.toastString.value = "Feedback submitted successfully."
                    Log.d(TAG, "initializeApiResponseObservers: Feedback submitted successfully.")
                    openFeedbackFinishDialog()
                }else if(it.status=="E") {
                    feedBackViewModel.toastString.value = "Something went wrong.Error.CreateSurvey."
                }else {
                    feedBackViewModel.toastString.value = "Something went wrong.CreateSurvey."
                }
            }
        }
    }

    private fun getModifiedSurveyData(surveyData: ArrayList<SurveyData>):ArrayList<ModifiedSurveyData> {
        return surveyData.map {
            ModifiedSurveyData(
                surveyData =it,
                rating = 0
            )

        } as ArrayList<ModifiedSurveyData>
    }

    private fun  renderQuestionList(questionListfromApi: ArrayList<SurveyData>) {
        Log.d(TAG, "renderQuestionList: ${questionList} ")
        feedBackViewModel.isProgressBarVisible.value = false
        feedBackViewModel.isRatingListVisible.value = true
        questionList.clear()
        questionList.addAll(getModifiedSurveyData(questionListfromApi))
        feedbackQuestionAdapter.notifyDataSetChanged()

    }

    private fun initializeFeedbackListAdapter()  {
        layoutManager = LinearLayoutManager(this)
        binding.rvRatingList.layoutManager = layoutManager
        feedbackQuestionAdapter = FeedbackQuestionAdapter(
            mContext = this,
            questionList
        )

        binding.rvRatingList.adapter = feedbackQuestionAdapter
    }

    fun showCommentDialog(question:String, position:Int,rating:Double) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.layout_feed_back_submit)
        val questionText = dialog.findViewById<AppCompatTextView>(R.id.tv_question)
        val commentEditText = dialog.findViewById<EditText>(R.id.edit_text_comment)

        val maxWords = 50 // Set the maximum number of words allowed

        val inputFilter = object : InputFilter {
            override fun filter(
                source: CharSequence?,
                start: Int,
                end: Int,
                dest: Spanned?,
                dstart: Int,
                dend: Int
            ): CharSequence? {
                // Convert the input to a string and split it into words
                val inputWords = dest?.toString()?.trim()?.split("\\s+".toRegex()) ?: emptyList()

                if (inputWords.size <= maxWords) {
                    // Accept the new input
                    return null
                }

                // Reject the new input
                return ""
            }
        }
        commentEditText.filters = arrayOf(inputFilter)

        val submitButton = dialog.findViewById<Button>(R.id.button_submit)


        questionText.text = question

        submitButton.setOnClickListener {
            if(commentEditText.text.toString().isNotEmpty()) {
                val comment = commentEditText.text.toString()
                feedbackQuestionAdapter.dataList[position].comment = comment
                feedbackQuestionAdapter.dataList[position].rating = rating.toInt()
                feedbackQuestionAdapter.notifyDataSetChanged()
                dialog.dismiss()
            }else {
                feedBackViewModel.toastString.value = "Feedback cannot be empty"
            }

        }

        val window = dialog.window
        window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)

        dialog.show()
    }

    private fun openFeedbackFinishDialog() {
        var feedbackSuccessDialog = Dialog(this)
        feedbackSuccessDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

//        joinVCDialog.context.setTheme(R.style.MyAlertDialogTheme)
        feedbackSuccessDialog.setContentView(R.layout.dialog_feedback_success)
        feedbackSuccessDialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        feedbackSuccessDialog.setCancelable(false)
        feedbackSuccessDialog!!.getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
        val okay = feedbackSuccessDialog.findViewById(R.id.btn_done) as MaterialButton

        okay.setOnClickListener {
            finish()
        }

        feedbackSuccessDialog.show()
    }
}
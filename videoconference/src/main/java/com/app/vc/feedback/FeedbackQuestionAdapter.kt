package com.kia.vc.feedback

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build.VERSION_CODES.P
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.R
import kotlin.math.log


private const val TAG = "FeedbackQuestionAdapter:"
class FeedbackQuestionAdapter(
     val mContext: Context,
    val dataList: ArrayList<ModifiedSurveyData>
):RecyclerView.Adapter<FeedbackQuestionAdapter.ViewHolder>() {

    var commentDialogListener: CommentDialogListener? = null

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var feedBackQuestion = itemView.findViewById<AppCompatTextView>(R.id.tv_question)
        var ratingBar = itemView.findViewById<RatingBar>(R.id.rating_feedback)
        var etComments = itemView.findViewById<EditText>(R.id.edit_text_comment)


        init {
            // Add an InputFilter to the etComments EditText
            etComments.filters = arrayOf(InputFilter.LengthFilter(50))
        }
//        var commentLayout = itemView.findViewById<LinearLayoutCompat>(R.id.ll_comments)
//        var comments = itemView.findViewById<AppCompatTextView>(R.id.tv_comments)
//        var commentsTitle = itemView.findViewById<AppCompatTextView>(R.id.tv_comments_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_feedback_item, parent, false)

        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder: ")
        var currentItem = dataList[position]
        holder.feedBackQuestion.text = currentItem.surveyData.surveyQuestion
        holder.ratingBar.rating = dataList[position].rating.toFloat()


//        holder.comments.text = currentItem.comment
//        if(currentItem.comment.isNotEmpty()) {
//            holder.commentLayout.visibility = View.VISIBLE
//        }else {
//            holder.commentLayout.visibility = View.GONE
//        }


        holder.ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            if (rating <= 3) {
//                commentDialogListener?.onCommentDialogRequested(currentItem.surveyData.surveyQuestion,position,rating.toDouble())
                holder.etComments.visibility = View.VISIBLE
                dataList[position].rating = rating.toInt()
            }else {

                holder.etComments.visibility = View.GONE
                Log.d(TAG, "onBindViewHolder: comments>3: ")
                dataList[position].rating = rating.toInt()

                dataList[position].comment = ""
                holder.etComments.text.clear()
//                notifyItemChanged(position)

            }
        }

        holder.etComments.addTextChangedListener(object :TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(p0: Editable?) {
                Log.d(TAG, "afterTextChanged: position:  ${position}")
                dataList[position].comment = p0.toString()

            }

        })
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    interface CommentDialogListener {
        fun onCommentDialogRequested(question: String, position: Int,rating:Double)
    }
}
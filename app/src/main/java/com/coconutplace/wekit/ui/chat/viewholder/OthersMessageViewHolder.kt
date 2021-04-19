package com.coconutplace.wekit.ui.chat.viewholder

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.coconutplace.wekit.R
import com.coconutplace.wekit.ui.chat.ChatMessageAdapter
import com.coconutplace.wekit.utils.ChatMessageUtil
import com.coconutplace.wekit.utils.SharedPreferencesManager.Companion.CHECK_TAG
import com.sendbird.android.BaseMessage
import com.sendbird.android.UserMessage

class OthersMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val dateLayout: LinearLayout = itemView.findViewById(R.id.chat_date_layout)
    private val dateText: TextView = itemView.findViewById(R.id.chat_date_text)
    private val sender: TextView = itemView.findViewById(R.id.chat_sender_text)
    private val textMessage: TextView = itemView.findViewById(R.id.chat_msg_text)
    private val sendTime: TextView = itemView.findViewById(R.id.chat_time_text)
    private val imageView: ImageView = itemView.findViewById(R.id.chat_profile_img)

    fun onBind(
        mContext: Context,
        message: BaseMessage,
        clickListener: ChatMessageAdapter.OnItemClickListener?,
        isNewDay: Boolean
    ) {
        sender.text = message.sender.nickname
        textMessage.text = message.message
        sendTime.text = ChatMessageUtil.formatTime(message.createdAt)

        if(isNewDay){
            dateLayout.visibility = View.VISIBLE
            dateText.text = ChatMessageUtil.formatDate(message.createdAt)
        }
        else{
            dateLayout.visibility = View.GONE
        }

        val profileUrl:String? = message.sender.profileUrl
        Log.e(CHECK_TAG,"profileUrl : $profileUrl")
        if(profileUrl==null||profileUrl==""){
            imageView.setImageResource(R.drawable.character_sm_basic)
        }
        else{
            //ChatMessageUtil.displayProfileWithPicasso(profileUrl,imageView)
            ChatMessageUtil.displayProfile(mContext,profileUrl,imageView)
        }
        if (clickListener != null) {
            //itemView.setOnClickListener { clickListener.onUserMessageItemClick(message as UserMessage?) }
            itemView.setOnClickListener { clickListener.onBackgroundClick() }
        }
    }


}
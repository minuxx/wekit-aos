package com.coconutplace.wekit.ui.chat

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.coconutplace.wekit.R
import com.coconutplace.wekit.data.remote.chat.listeners.ChatListener
import com.coconutplace.wekit.data.remote.chat.listeners.DialogListener
import com.coconutplace.wekit.databinding.ActivityChatBinding
import com.coconutplace.wekit.ui.BaseActivity
import com.coconutplace.wekit.ui.WekitV2Dialog
import com.coconutplace.wekit.ui.chat.dialog.*
import com.coconutplace.wekit.ui.member_gallery.MemberGalleryActivity
import com.coconutplace.wekit.ui.photo_viewer.PhotoViewerActivity
import com.coconutplace.wekit.ui.write_diary.WriteDiaryActivity
import com.coconutplace.wekit.utils.GlobalConstant.Companion.DUMMY_MESSAGE_COUNT
import com.coconutplace.wekit.utils.GlobalConstant.Companion.FLAG_CERTIFY_DIARY
import com.coconutplace.wekit.utils.GlobalConstant.Companion.FLAG_LEAVE_CHANNEL
import com.coconutplace.wekit.utils.GlobalConstant.Companion.REQ_CODE_AUTH_IMAGE
import com.coconutplace.wekit.utils.GlobalConstant.Companion.REQ_CODE_SELECT_IMAGE
import com.coconutplace.wekit.utils.GlobalConstant.Companion.RES_CODE_AUTH_FAILURE
import com.coconutplace.wekit.utils.GlobalConstant.Companion.RES_CODE_AUTH_SUCCESS
import com.coconutplace.wekit.utils.PushUtil
import com.coconutplace.wekit.utils.SharedPreferencesManager.Companion.CHECK_TAG
import com.coconutplace.wekit.utils.SharedPreferencesManager.Companion.ERROR_TAG
import com.coconutplace.wekit.utils.hideKeyboard
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.sendbird.android.BaseMessage
import com.sendbird.android.FileMessage
import com.sendbird.android.SendBird
import com.sendbird.android.SendBird.SetPushTriggerOptionHandler
import com.sendbird.android.UserMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.net.URLConnection
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : BaseActivity(),ChatListener, DialogListener,WekitV2Dialog.WekitV2DialogClickListener {

    private lateinit var binding: ActivityChatBinding//layout????????? ????????? ???????????????( _ ????????? ???????????? ????????? + Binding)
    private val chatViewModel: ChatViewModel by viewModel()
    private lateinit var messageAdapter: ChatMessageAdapter
    private lateinit var memberListAdapter: ChatMemberListAdapter

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)

        val channelUrl = intent.getStringExtra("channelUrl")!!
        val roomIdx = intent.getIntExtra("roomIdx",0)

        chatViewModel.setChatListener(this)
        chatViewModel.init(channelUrl,roomIdx)

        setupView()
        setupChatListAdapter()

        setupDrawer() //????????? ????????? ????????? drawer ??????
        setupMemberListAdapter()

        setupViewModel()

    }

    override fun onResume() {
        super.onResume()

        if (binding.chatDrawerLayout.isDrawerOpen(GravityCompat.END)) { //drawer??? ???????????? ??? ??????????????? ?????? ??????
            displayMenu()
            binding.chatDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        }
        else{ //drawer??? ???????????? ??? ??????????????? ????????????
            binding.chatDrawerLayout.setDrawerLockMode((DrawerLayout.LOCK_MODE_LOCKED_CLOSED))
        }
    }

    private fun displayMenu(){ //drawer?????? ????????? ????????????
        memberListAdapter.clear()
        binding.chatNavListView.adapter = memberListAdapter

        chatViewModel.getRoomInfo()
    }

    private fun setupView(){

        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat)
        binding.lifecycleOwner = this
        binding.mChatViewModel = chatViewModel

        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.reverseLayout = true //???????????? ??????

        binding.chatRecyclerview.layoutManager = linearLayoutManager

        messageAdapter = ChatMessageAdapter()

        binding.chatRecyclerview.adapter = messageAdapter
        binding.chatRecyclerview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (linearLayoutManager.findLastVisibleItemPosition() == messageAdapter.itemCount-1) {
                    chatViewModel.loadPreviousMessages(DUMMY_MESSAGE_COUNT, null)
                }
            }
        })

        binding.chatSendButtonFrameLayout.setOnClickListener{
            val msg : String = binding.chatMsgEt.text.toString()
            if(msg==""){
                return@setOnClickListener
            }
            chatViewModel.sendMsg(msg)
            binding.chatMsgEt.setText("")
        }
        binding.chatSendButton.setOnClickListener{
            val msg : String = binding.chatMsgEt.text.toString()
            if(msg==""){
                return@setOnClickListener
            }
            chatViewModel.sendMsg(msg)
            binding.chatMsgEt.setText("")
        }

        binding.chatMenuButtonFrameLayout.setOnClickListener{
            binding.root.hideKeyboard()
            binding.chatDrawerLayout.openDrawer(GravityCompat.END)
            displayMenu()
        }
        binding.chatMenuButton.setOnClickListener{
            binding.root.hideKeyboard()
            binding.chatDrawerLayout.openDrawer(GravityCompat.END)
            displayMenu()
        }

        binding.chatBackButtonFrameLayout.setOnClickListener{
            finish()
        }
        binding.chatBackButton.setOnClickListener{
            finish()
        }

        binding.chatSendPictureButtonFrameLayout.setOnClickListener{
            //requestMedia()
            val imgTypeDialog = ChatImgTypeDialog(this)
            imgTypeDialog.callFunction(this)
        }
        binding.chatSendPictureButton.setOnClickListener{
            //requestMedia()
            val imgTypeDialog = ChatImgTypeDialog(this)
            imgTypeDialog.callFunction(this)
        }
    }

    private fun setupDrawer(){ //drawer????????????
        binding.chatDrawerLayout.addDrawerListener(object:DrawerListener{
            override fun onDrawerOpened(drawerView: View) {
                binding.chatDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            }
            override fun onDrawerClosed(drawerView: View) {
                binding.chatDrawerLayout.setDrawerLockMode((DrawerLayout.LOCK_MODE_LOCKED_CLOSED))
            }
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })

        binding.chatNavMenuBtn.setOnClickListener{
            val actionTypeDialog = ChatActionTypeDialog(this)
            actionTypeDialog.callFunction(this)
        }
        binding.chatNavExitBtn.setOnClickListener{
            val exitDialog = WekitV2Dialog(this, FLAG_LEAVE_CHANNEL)
            exitDialog.listener = this
            exitDialog.show(getString(R.string.chat_dialog_exit_title))
        }

        if(chatViewModel.getPushFlag()){
            binding.chatNavBellBtn.setImageResource(R.drawable.icn_bell_on)
            R.drawable.icn_bell_off
        }

        binding.chatNavBellBtn.setOnClickListener {
            if(chatViewModel.getPushFlag()){
                PushUtil.setPushNotification(false, //???????????? ON ??????
                    SetPushTriggerOptionHandler { e ->
                        if(e!=null){
                            Log.e(ERROR_TAG,"push notification OFF setting error $e")
                            return@SetPushTriggerOptionHandler
                        }
                        chatViewModel.setPushFlag(false)
                        binding.chatNavBellBtn.setImageResource(R.drawable.icn_bell_off)
                    })
            }
            else{
                PushUtil.setPushNotification(true, //???????????? OFF ??????
                    SetPushTriggerOptionHandler { e ->
                        if(e!=null){
                            Log.e(ERROR_TAG,"push notification ON setting error $e")
                            return@SetPushTriggerOptionHandler
                        }
                        chatViewModel.setPushFlag(true)
                        binding.chatNavBellBtn.setImageResource(R.drawable.icn_bell_on)
                    })
            }
        }
    }

    private fun setupChatListAdapter(){ //?????? ???????????? ?????? ????????? ??????
        messageAdapter.setItemClickListener(object : ChatMessageAdapter.OnItemClickListener {
            override fun onUserMessageItemClick(message: UserMessage?) {
                Log.e(CHECK_TAG,"USER message ?????????")
            }
            override fun onFileMessageItemClick(message: FileMessage?) {
                Log.e(CHECK_TAG,"File message ?????????")
                val type = message!!.type.toLowerCase(Locale.ROOT)
                if (type.startsWith("image")) {
                    onImageMessageClicked(message)
                }
            }
            override fun onBackgroundClick() {
                Log.e(CHECK_TAG,"background item ?????????")
                binding.root.hideKeyboard()
            }

            override fun onProfileClick(profileUrl: String?) {
                onProfileClicked(profileUrl!!)
            }

        })
    }

    private fun setupMemberListAdapter(){ //drawer??? ??????????????? ?????? ????????? ??????
        memberListAdapter = ChatMemberListAdapter()

        memberListAdapter.setItemClickListener(object: ChatMemberListAdapter.OnItemClickListener{
            override fun onItemClick(memberInfo: ChatMemberListAdapter.MemberInfo) {
                Log.e(CHECK_TAG,memberInfo.nickName)

                val intent = Intent(this@ChatActivity,MemberGalleryActivity::class.java)
                intent.putExtra("userIdx",memberInfo.userIdx)
                intent.putExtra("roomIdx",chatViewModel.getRoomIdx())
                intent.putExtra("nickName",memberInfo.nickName)
                startActivity(intent)
            }
        })
    }

    private fun setupViewModel(){
        chatViewModel.refresh()
        chatViewModel.addSendBirdHandler()

        chatViewModel.isInitialized.observe(this,{
            if(it==true) { //?????? n??? ???????????? ??????????????? ????????? ???????????? ??????
                binding.chatRecyclerview.scrollToPosition(0)
                Log.e(CHECK_TAG, "recyclerview position :${(messageAdapter.itemCount - 1)}")
            }
        })

        chatViewModel.liveMemberListInfo.observe(this,{
            memberListAdapter.clear()
            binding.chatNavListView.adapter = memberListAdapter

            for(member in it){
                memberListAdapter.addItem(member.countNum,member.nickname,member.type, member.todayCount, member.userIdx)
                Log.e(CHECK_TAG,"ListView addItem : ${member.nickname} ?????????")
            }
            memberListAdapter.notifyDataSetChanged()

            Log.e(CHECK_TAG,"ListView addItem count : "+memberListAdapter.count)
        })

        chatViewModel.isLoading.observe(this,{
            if(it){
                binding.chatProgressbar.visibility = View.VISIBLE
            }
            else{
                binding.chatProgressbar.visibility = View.GONE
            }
        })

        chatViewModel.showDialog.observe(this,{ event ->
            event.getContentIfNotHandled()?.let {
                makePopup(it)
            }
        })
    }

    //?????? ????????? ????????? ??????
    private fun onImageMessageClicked(message : FileMessage){
        val intent = Intent(this,PhotoViewerActivity::class.java)
        intent.putExtra("url", message.url)
        intent.putExtra("type", message.type)
        startActivity(intent)
    }

    //?????? ????????? ????????? ??????
    private fun onProfileClicked(profileUrl: String){
        val intent = Intent(this,PhotoViewerActivity::class.java)
        intent.putExtra("url", profileUrl)
        startActivity(intent)
    }

    //?????? ????????? ?????? ???????????????
    private fun requestMedia() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, REQ_CODE_SELECT_IMAGE)
        SendBird.setAutoBackgroundDetection(false)
    }

    private fun makePopup(str: String) {
        runOnUiThread{
            showDialog(str)
        }
    }

    //????????? ????????? ????????? ?????????(??? ???????????? ?????? ???)
    override fun onBadgeResponse(badgeTitle:String,badgeUrl:String,badgeExplain:String, backgroundColor:String){
        runOnUiThread {
            val badgeDialog = ChatBadgeDialog(this)
            badgeDialog.callFunction(badgeTitle,badgeUrl,badgeExplain,backgroundColor)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onActivityResult(requestCode:Int, resultCode:Int, data:Intent?){
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQ_CODE_SELECT_IMAGE){ //?????? ?????? ????????? ?????? ????????? ?????? ?????? ???
            if(resultCode==Activity.RESULT_OK){
                SendBird.setAutoBackgroundDetection(true)
                val uri:Uri = data!!.data!!
                Log.e(CHECK_TAG,uri.toString())
                chatViewModel.sendFile(uri,this)
            }
        }
        else if(requestCode == REQ_CODE_AUTH_IMAGE){ //???????????? ????????? ?????? Diary?????? ?????? ???
            if(resultCode== RES_CODE_AUTH_SUCCESS){
                //writeDiaryActivity?????? ???????????? sendbird ???????????? ????????????
                Log.e(CHECK_TAG,"writeDiary?????? imgUrl??? ??????????????????.")
                val urlArray:ArrayList<String> = data?.getSerializableExtra("imgList") as ArrayList<String>
//                val satisfaction:Int = data.getIntExtra("satisfaction",0)
//                val date:String? = data.getStringExtra("date")
//                val timezone:Int = data.getIntExtra("timezone",0)
//                val memo:String? = data.getStringExtra("memo")

                for(url in urlArray){
                    Log.e(CHECK_TAG,"url : $url")
                }
                //????????? ????????? ?????????
                if(urlArray.size>0){
                    sendFileWithUrl(urlArray[0])
                    //mChatViewModel.postDiaryWithChat(urlArray,satisfaction,date!!,timezone,memo!!)

                    val badgeTitle = data.getStringExtra("badgeTitle")
                    if(badgeTitle != null){
                        val badgeUrl = data.getStringExtra("badgeUrl")
                        val badgeExplain = data.getStringExtra("badgeExplain")
                        val backgroundColor = data.getStringExtra("backgroundColor")

                        val badgeDialog = ChatBadgeDialog(this)
                        badgeDialog.callFunction(badgeTitle,badgeUrl!!,badgeExplain!!,backgroundColor!!)
                    }
                }

            }
            else if(resultCode== RES_CODE_AUTH_FAILURE){
                Log.e(ERROR_TAG,"writeDiary ?????? imgUrl??? ???????????? ???????????????.")
                makePopup("?????????????????? ????????? ???????????? ???????????????")
            }
        }
    }

    override fun onBackPressed() { //?????????????????? back button ???????????? drawer??????????????? ??????
        if (binding.chatDrawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.chatDrawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }

    override fun onExitSuccess() { //????????? ????????? ?????????
        finish()
    }

    override fun onSendMessageSuccess() { //??????????????? ????????? ?????? ????????? ????????? ????????? ??????
        binding.chatRecyclerview.scrollToPosition(0)
    }

    override fun showStartChallengeButton(isHost: Boolean) { //????????? ????????? ??????????????? ????????? ????????? ????????????
        runOnUiThread{
            if(isHost){
                binding.chatNavStartChallengeBtn.visibility = View.VISIBLE
            }
            else{
                binding.chatNavStartChallengeBtn.visibility = View.INVISIBLE
            }
        }
    }

    //????????? ?????? ????????? ?????? ????????? ??????
    override fun addOldMsg(msgList: List<BaseMessage>) {
        messageAdapter.addFirst(msgList)
        messageAdapter.notifyDataSetChanged()
    }

    //????????? ????????? ????????? ??????
    override fun addRecentMessage(msg: BaseMessage) {
        messageAdapter.addLast(msg)
        messageAdapter.notifyDataSetChanged()
        binding.chatRecyclerview.scrollToPosition(0)
    }

    //????????? ???????????? ????????????/???????????? ??????????????????
    override fun getBackImgTypeDialog(type: Int) {
        if(type==1){ //?????? ?????? ?????????
            Log.e(CHECK_TAG,"???????????? ????????? ??????")
            chatViewModel.checkChallenge()
        }
        else if(type==2){ //?????? ?????? ?????????
            Log.e(CHECK_TAG,"???????????? ????????? ??????")
            requestMedia()
        }
    }

    override fun getBackActionTypeDialog(type: Int) {
        when(type){
            1 ->{ //??? ???????????? ?????????
                val reportDialog = ChatReportDialog(this)
                reportDialog.callFunction(this)
            }
            2 ->{ //?????? ???????????? ?????????
                if(chatViewModel.isChallengeable){
                    val expelDialog = ChatExpelDialog(this,chatViewModel.getNickName())
                    expelDialog.callFunction(this, chatViewModel.getMemberInfoList())
                }
                else{
                    makePopup("????????? ????????? ??? ??? ????????????.")
                }
            }
        }
    }

    //????????? ?????? ???????????? ???
    override fun getBackExpelDialog(member:String, reason:String) {
        chatViewModel.expelMember(member,reason)
    }

    //??? ?????? ?????? ???????????? ???
    override fun getBackReportDialog(reason:String) {
        chatViewModel.reportChannel(reason)
    }

    //??? ????????? ???????????? ???
    override fun getBackExitDialog(exitFlag: Boolean) {
        if(exitFlag){
            chatViewModel.exitChannel()
        }
    }

    override fun startDiary() { //???????????????????????? ??????????????? ???
        val intent = Intent(this, WriteDiaryActivity::class.java)
        val date = CalendarDay.today()
        val month = if (date.month < 10) {"0" + date.month} else { date.month.toString() }
        val day = if (date.day < 10) {"0" + date.day} else { date.day.toString() }

        intent.putExtra("date", "${date.year}-${month}-${day}")
        intent.putExtra("flag", FLAG_CERTIFY_DIARY)
        intent.putExtra("roomIdx",chatViewModel.getRoomIdx())
        startActivityForResult(intent,REQ_CODE_AUTH_IMAGE)
    }

    private fun sendFileWithUrl(ImgUrl:String){//SendBird??? ?????? ????????? ???
        chatViewModel.isLoading.postValue(true)
        CoroutineScope(Dispatchers.Default).launch {
            runCatching {
                try {
                    val url = URL(ImgUrl)
                    val conn: URLConnection = url.openConnection()
                    conn.connect()
                    val bis = BufferedInputStream(conn.getInputStream())
                    val imgBitmap = BitmapFactory.decodeStream(bis)
                    bis.close()

                    val fileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(Date()) + ".jpg"
                    val file = getFileFromBitmap(imgBitmap!!, fileName)
                    Log.e(CHECK_TAG, "????????????:$fileName")
                    chatViewModel.sendAuthFile(file!!, fileName)

                } catch (e: Exception) {
                    Log.e(ERROR_TAG, "?????? ????????? ??????$e")
                }
            }
        }
    }

    //????????? ????????? ??? ?????? ?????????
    private fun getFileFromBitmap(imgBitmap:Bitmap,fileName:String):File?{
        val storage: File = cacheDir

        val tempFile = File(storage, fileName)

        try{
            tempFile.createNewFile()
            val out = FileOutputStream(tempFile)
            imgBitmap.compress(Bitmap.CompressFormat.JPEG,100,out)
            out.close()
        }
        catch (e:Exception){
            Log.e(ERROR_TAG,"????????? ?????? ?????? $e")
        }

        val file = File(cacheDir.toString())
        val files = file.listFiles()!!
        for(imgFile:File in files){
            if(imgFile.name==fileName){
                Log.e(CHECK_TAG,"?????? ???????????????.")
                return imgFile
            }
        }
        Log.e(ERROR_TAG,"????????? ?????? ??????????????????")
        return null
    }

    override fun onOKClicked(flag: Int) {
        when(flag){
            FLAG_LEAVE_CHANNEL -> chatViewModel.exitChannel()
        }
    }

}


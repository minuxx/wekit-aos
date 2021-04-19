package com.coconutplace.wekit.utils

class GlobalConstant {
    companion object{
        //common
        const val FIREBASE_STORAGE_URL = "gs://wekit-a56e6.appspot.com/"
        const val DEBUG_TAG = "DEBUG://"
        const val FLAG_NETWORK_ERROR = 404

        //splash
        const val FLAG_SERVER_CHECK = 776
        const val FLAG_VERSION_UPDATE = 777

        //sendbird
        const val APP_ID = "9466BEF9-EC21-46D4-ACD9-98BC069551D7"

        //diary
        const val IMAGE_PICK_CODE = 1000
        const val SATISFACTION_HAPPY = 1
        const val SATISFACTION_SPEECHLESS = 2
        const val SATISFACTION_SAD = 3
        const val SATISFACTION_ANGRY = 4

        const val TIMEZONE_BREAKFAST = 1
        const val TIMEZONE_BLUNCH = 2
        const val TIMEZONE_LUNCH = 3
        const val TIMEZONE_LINNER = 4
        const val TIMEZONE_DINNER = 5

        const val FLAG_CERTIFY_DIARY = 50
        const val FLAG_WRITE_DIARY = 51
        const val FLAG_READ_DIARY = 52

        const val REQUEST_PHOTOS = 55

        //choice photo
        const val REQUEST_SELECT_PICTURE = 122
        const val REQUEST_TAKE_PICTURE = 123
        const val ITEM_TYPE_ADD_PHOTO = 432
        const val ITEM_TYPE_PHOTO = 433

        //body graph
        const val BODY_GRAPH_FRAGMENT_CNT = 2

        //chat
        const val REQ_CODE_SELECT_IMAGE = 1001
        const val REQ_CODE_AUTH_IMAGE = 1002
        const val RES_CODE_AUTH_SUCCESS = 101
        const val RES_CODE_AUTH_FAILURE = 102
        const val DUMMY_MESSAGE_COUNT = 20

        //set
        const val PROFILE_URL = "PROFILE_IMG"
        const val FLAG_TUTORIAL_SET = 321
        const val FLAG_TUTORIAL_SIGNUP = 320

        //certify email
        const val FLAG_CERTIFY_EMAIL = 601
        const val FLAG_CERTIFY_NUMBER = 602
    }
}
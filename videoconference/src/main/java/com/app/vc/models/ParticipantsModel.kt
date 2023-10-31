package com.app.vc.models

import org.webrtc.VideoTrack

/* created by Naghma 27/09/23*/


class ParticipantsModel(
    var trackId:String,
    var streamId:String,
    var isLocal:Boolean,
    var isMicOn:Boolean,
    var isCamOn:Boolean,
    var track: VideoTrack?
){
    var localName = "ABC"
}
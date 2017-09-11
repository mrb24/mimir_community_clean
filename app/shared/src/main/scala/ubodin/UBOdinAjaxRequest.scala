package ubodin

//import scala.reflect.runtime.universe.Type

abstract class UBOdinAjaxRequest(val requestID:String) 

object UBOdinAjaxRequest {
  def readRequest(requestID:String, request:String) : UBOdinAjaxRequest = {
    requestID match {
      case "NoRequest" => upickle.read[NoRequest](request)
      case "CleaningJobListRequest" => upickle.read[CleaningJobListRequest](request)
      case "CleaningJobTaskListRequest" => upickle.read[CleaningJobTaskListRequest](request)
      case "CleaningJobTaskFocusedListRequest" => upickle.read[CleaningJobTaskFocusedListRequest](request)
      case "CleaningJobDataRequest" => upickle.read[CleaningJobDataRequest](request)
      case "UserInfoRequest" => upickle.read[UserInfoRequest](request)
      case "LoginRequest" => upickle.read[LoginRequest](request)
      case "CleaningJobTaskAcknowledgeRequest" => upickle.read[CleaningJobTaskAcknowledgeRequest](request)
      case "CleaningJobTaskRepairRequest" => upickle.read[CleaningJobTaskRepairRequest](request)
      case "CreateCleaningJobLocationFilterSettingRequest" => upickle.read[CreateCleaningJobLocationFilterSettingRequest](request)
      case "RemoveCleaningJobLocationFilterSettingRequest" => upickle.read[RemoveCleaningJobLocationFilterSettingRequest](request)
      case "GetCleaningJobSettingsRequest" => upickle.read[GetCleaningJobSettingsRequest](request)
      case "GetCleaningJobSettingsOptionsRequest" => upickle.read[GetCleaningJobSettingsOptionsRequest](request)
      case "LoadCleaningJobRequest" => upickle.read[LoadCleaningJobRequest](request)
      case "CleaningJobRepairRequest" => upickle.read[CleaningJobRepairRequest](request)
      case "SetDeviceLocationRequest" => upickle.read[SetDeviceLocationRequest](request)
    }
  }
}


case class NoRequest() extends UBOdinAjaxRequest("NoRequest") 

case class CleaningJobListRequest(deviceID: String) extends UBOdinAjaxRequest("CleaningJobListRequest") 

case class CleaningJobTaskListRequest(deviceID: String, cleaningJobID: String, count: Int, offset:Int) extends UBOdinAjaxRequest("CleaningJobTaskListRequest") 

case class CleaningJobTaskFocusedListRequest(deviceID: String, cleaningJobID: String, rowIDs:Seq[String], cols:Seq[String]) extends UBOdinAjaxRequest("CleaningJobTaskFocusedListRequest") 

case class CleaningJobDataRequest(deviceID: String, cleaningJobID: String, count: Option[Int], offset:Option[Int]) extends UBOdinAjaxRequest("CleaningJobDataRequest")

case class UserInfoRequest(deviceID: String) extends UBOdinAjaxRequest("UserInfoRequest")

case class LoginRequest(deviceID: String) extends UBOdinAjaxRequest("LoginRequest")

case class CleaningJobTaskAcknowledgeRequest(cleaningJobID: String  ) extends UBOdinAjaxRequest("CleaningJobTaskAcknowledgeRequest") 

case class CleaningJobTaskRepairRequest(cleaningJobID: String) extends UBOdinAjaxRequest("CleaningJobTaskRepairRequest") 

case class CreateCleaningJobLocationFilterSettingRequest(deviceID: String, cleaningJobDataID: String, name:String, distance:Double, latCol:String, lonCol:String, lat:Double, lon:Double) extends UBOdinAjaxRequest("CreateCleaningJobLocationFilterSettingRequest") 

case class RemoveCleaningJobLocationFilterSettingRequest(deviceID: String, cleaningJobDataID: String, name:String) extends UBOdinAjaxRequest("RemoveCleaningJobLocationFilterSettingRequest") 

case class GetCleaningJobSettingsRequest(deviceID: String, cleaningJobID: String) extends UBOdinAjaxRequest("GetCleaningJobSettingsRequest") 

case class GetCleaningJobSettingsOptionsRequest(cleaningJobID: String) extends UBOdinAjaxRequest("GetCleaningJobSettingsOptionsRequest") 

case class LoadCleaningJobRequest(deviceID:String, cleaningJobID: String) extends UBOdinAjaxRequest("LoadCleaningJobRequest") 

case class SetDeviceLocationRequest(deviceID:String, lat:Double, lon:Double) extends UBOdinAjaxRequest("SetDeviceLocationRequest") 

case class CleaningJobRepairRequest(deviceID:String, cleaningJobID:String, model:String, idx:Int, args:Seq[String], repairValue:String) extends UBOdinAjaxRequest("CleaningJobRepairRequest") 
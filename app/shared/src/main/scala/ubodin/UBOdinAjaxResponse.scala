package ubodin

abstract class UBOdinAjaxResponse(val responseID : String) 

object UBOdinAjaxResponse {
  def readResponse(responseID:String, response:String) : UBOdinAjaxResponse = {
    responseID match {
      case "NoResponse" => upickle.read[NoResponse](response)
      case "CleaningJobListResponse" => upickle.read[CleaningJobListResponse](response)
      case "CleaningJobDataResponse" => upickle.read[CleaningJobDataResponse](response)
      case "UserInfoResponse" => upickle.read[UserInfoResponse](response)
      case "LoginResponse" => upickle.read[LoginResponse](response)
      case "GetCleaningJobSettingsResponse" => upickle.read[GetCleaningJobSettingsResponse](response)
      case "GetCleaningJobSettingsOptionsResponse" => upickle.read[GetCleaningJobSettingsOptionsResponse](response)
      case "LoadCleaningJobResponse" => upickle.read[LoadCleaningJobResponse](response)
    }
  }
}

case class NoResponse() extends UBOdinAjaxResponse("NoResponse")

case class CleaningJobListResponse(cleaningJobs: Vector[CleaningJob]) extends UBOdinAjaxResponse("CleaningJobListResponse")

case class CleaningJobTaskListResponse(cleaningJobTasks: Vector[CleaningJobTaskGroup]) extends UBOdinAjaxResponse("CleaningJobTaskListResponse")

case class CleaningJobDataResponse(cleaningJobData: Vector[CleaningJobData]) extends UBOdinAjaxResponse("CleaningJobDataResponse")

case class GetCleaningJobSettingsResponse(cleaningJobSettings: Vector[CleaningJobSettingContainer]) extends UBOdinAjaxResponse("GetCleaningJobSettingsResponse")

case class GetCleaningJobSettingsOptionsResponse(cleaningJobSettingsOptions: Vector[SettingOptionContainer]) extends UBOdinAjaxResponse("GetCleaningJobSettingsOptionsResponse")

case class UserInfoResponse(userInfo: UserInfoData) extends UBOdinAjaxResponse("UserInfoResponse")

case class LoginResponse(userInfo:UserInfoResponse, cleaningJobs:CleaningJobListResponse)  extends UBOdinAjaxResponse("LoginResponse")

case class LoadCleaningJobResponse(job:CleaningJob, options:GetCleaningJobSettingsOptionsResponse, settings:GetCleaningJobSettingsResponse, taskList:CleaningJobTaskListResponse, data:CleaningJobDataResponse) extends UBOdinAjaxResponse("LoadCleaningJobResponse")
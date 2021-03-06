//This will get number of folders in destinantion directory
def numberOfJobs = NUMBER_OF_FOLDERS.toInteger()

def threshold = SAD_THRESHOLD.toInteger()

// This will get the folder name in timestamp format where all jobs will be created
def name=folderName()

//This will create root folder
folder(name){

for(i = 1; i <= numberOfJobs; i++){
  
  def jobPath = name + "/" + name + "_" + "AudioAnalysisJob" + i
  
  def folderPath = AUDIO_FILES_DESTINATION_DIRECTORY + "/job" + i
  
  println jobPath
  println folderPath
  
    //change here
    job(jobPath) {
      label('!master')
      //steps      
      publishers {
        downstreamParameterized {
            trigger('CopyCsvFilesToProcessedDirectory') {
               
                triggerWithNoParameters false
                condition('UNSTABLE_OR_BETTER')
               
                parameters {
                    predefinedProp('PARENT_WORKSPACE', '${WORKSPACE}')
                    predefinedProp('PROCESSSED_CSV_DIRECTORY', '/mnt/shared/data/processed_csv_destination')
                }
            }
        }
      }
      
      configure { project ->
        
      project.name = 'com.citycomsolutions.jenkins.CitycomProject'
              
      project / scm (class: 'hudson.plugins.filesystem_scm.FSSCM') {
            //change here
        path folderPath
            clearWorkspace false
      }
        
      project / analyzerOptions {
            loggingLevel 3
      }
    
      project / analyzerOptions / elkAnalyzerOptions {
            consoleToLogstash true
            //elkName 'None'
            incrementalAnalysis true
            numberOfAnalysisToMantain 5
      }
        
      project / analyzerOptions / ffmpegAnalyzerOptions {
            ffmpegName 'FFmpeg 3.4.2'
            ignoreUnknownFormats true
      }
        
      project / analyzerOptions / pythonAudioQualityAnalyzerOptions {
            pythonName 'Python 3.5.2'
            //change here
            sadThreshold threshold
      }
    }     
  }
  
  if (!jenkins.model.Jenkins.instance.getItemByFullName(jobPath)) {
    queue(jobPath)
  }
}
}


def folderName(){
  def today = new Date()
  def sdf = new java.text.SimpleDateFormat("yyyy-MM-dd-hh-mm-ss")
  
  return sdf.format(today)
}

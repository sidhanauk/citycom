manage()
{
job("00.StartUpJob") {
    label('master')
	description()
	keepDependencies(false)
	parameters {
		stringParam("AUDIO_FILES_LANDING_DIRECTORY", "/mnt/public/data/landing", "")
		stringParam("AUDIO_FILES_DESTINATION_DIRECTORY", "/mnt/public/data/destination", "")
		choiceParam("DIR_SIZE_IN_MB", [250, 1, 5, 10, 50, 100, 150, 200, 300], "Directory size in MegaBytes")
        choiceParam("SAD_THRESHOLD", [-45, -40, -35, -30, -25, -20, -15, -10, -5], "Threshold decible value")
    }
	disabled(false)
	concurrentBuild(false)
	steps {
		shell("echo \"This is the start-up job\"")
  
        downstreamParameterized {
          trigger('01.countFilesInLandingDirectory') {
            
            block {
              buildStepFailure('FAILURE')
              failure('FAILURE')
              unstable('UNSTABLE')
            }
            
            parameters {
                currentBuild()
            }
          }
        }
    }
}

job("01.countFilesInLandingDirectory") {
    label('master')
	description("This job will list all different files types with their file numbers")
	keepDependencies(false)
	parameters {
		stringParam("AUDIO_FILES_LANDING_DIRECTORY", "/mnt/public/data/landing", "")
		stringParam("AUDIO_FILES_DESTINATION_DIRECTORY", "/mnt/public/data/destination", "")
		choiceParam("DIR_SIZE_IN_MB", [250, 1, 5, 10, 50, 100, 150, 200, 300], "Directory size in MegaBytes")
        choiceParam("SAD_THRESHOLD", [-45, -40, -35, -30, -25, -20, -15, -10, -5], "Threshold decible value")
    }
	disabled(false)
	concurrentBuild(false)
	steps {
		shell("""#!/usr/bin/env bash +x

        echo \${AUDIO_FILES_LANDING_DIRECTORY}
        echo \${AUDIO_FILES_DESTINATION_DIRECTORY}

        cd \${AUDIO_FILES_LANDING_DIRECTORY}

        # Now print current working directory
        pwd

        fileCountLanding=\$(find . -type f|wc -l)

		

		echo "Total number of files in Landing directory: \${fileCountLanding}"


        if [ \${fileCountLanding} -gt 0 ] 
        then
echo "FILE_COUNT_LANDING=\${fileCountLanding}" > /var/lib/jenkins/jobs/01.countFilesInLandingDirectory/workspace/env.properties

            echo "############################################"
            echo "Files found now print file types"
            echo "********************************************"
            echo "********************************************"
            find . -type f | sed -e 's/.*\\.//' | sort | uniq -c | sort -n
            echo "********************************************"
            echo "********************************************"
        else
            echo "############################################"
            echo "Exit: No files found in Landing directory"
            echo "############################################"
            exit 1
        fi""")
    
        downstreamParameterized {
          trigger('02.createDestinationDirectoryStructure') {
            
            block {
              buildStepFailure('FAILURE')
              failure('FAILURE')
              unstable('UNSTABLE')
            }
            
            parameters {
                currentBuild()
                propertiesFile('${WORKSPACE}/env.properties', true)
            }
          }
        }
    }
}

job("02.createDestinationDirectoryStructure") {
    label('master')
	description()
	keepDependencies(false)
	parameters {
		stringParam("AUDIO_FILES_LANDING_DIRECTORY", "/mnt/public/data/landing", "LANDING DIRECTORY: Path on shared drive where audio files are sourced")
		stringParam("AUDIO_FILES_DESTINATION_DIRECTORY", "/mnt/public/data/destination", "DESTINATION DIR: Path on shared drive where audio files are moved for processing")
	    choiceParam("DIR_SIZE_IN_MB", [250, 1, 5, 10, 50, 100, 150, 200, 300], "Directory size in MegaBytes")
        choiceParam("SAD_THRESHOLD", [-45, -40, -35, -30, -25, -20, -15, -10, -5], "Threshold decible value")
      	stringParam("FILE_COUNT_LANDING", "", "")
	}
	disabled(false)
	concurrentBuild(false)
	steps {
		shell("""#!/usr/bin/env bash

echo "FILE_COUNT_LANDING: \$FILE_COUNT_LANDING"

SAVEIFS=\$IFS
IFS=\$(echo -en "\\n\\b")

# Now clean the destination directory
rm -rf \${AUDIO_FILES_DESTINATION_DIRECTORY}

if [ -d \${AUDIO_FILES_LANDING_DIRECTORY} ]; then

  # Now go to directory where audio files are present
  cd \${AUDIO_FILES_LANDING_DIRECTORY}

  #Print present working directory
  pwd
  
  DIRECTORY_SIZE=\$((DIR_SIZE_IN_MB*1000000))
  
  # Create empty 100 job folders
  echo "Create folder structure"
  for i in {1..1000}; do mkdir -p \${AUDIO_FILES_DESTINATION_DIRECTORY}/job\$i; done  
  
  echo "Move files to job folders until the folder size is ~250MB"
  for i in {1..1000}
  do
    for j in `find . -type f`
    do
      mv "\${j}" "\${AUDIO_FILES_DESTINATION_DIRECTORY}/job\$i"
  
      CHECK=\$(du -sb \${AUDIO_FILES_DESTINATION_DIRECTORY}/job\$i | cut -f1)

	  size_in_mb=\${CHECK}/1000000
      
      echo "Directory size: \${size_in_mb} MB"
  
      if [ \$CHECK -gt \${DIRECTORY_SIZE} ]; then
        break;
      fi
    done
  done
else
	echo "Landing folder is not found"
    exit 1
fi


#Now delete empty folders in destinantion folder
if [ -d \${AUDIO_FILES_DESTINATION_DIRECTORY} ]; then

  cd \${AUDIO_FILES_DESTINATION_DIRECTORY}

  emptyDirCount=\$(find . -type d -empty|wc -l)

  if [ \${emptyDirCount} -gt 0 ]; then
      find . -type d -empty -delete
  fi
else
	echo "Destination folder is not found"
  exit 1
fi

echo "*** Folder structure in destinantion directory ***"
echo "**************************************************"
ls -ltr""")
	
     
      downstreamParameterized {
        trigger('03.countFilesInDestinationDirectory') {
          
          block {
            buildStepFailure('FAILURE')
            failure('FAILURE')
            unstable('UNSTABLE')
          }
          
          parameters {
              currentBuild()
            "FILE_COUNT_LANDING: \$FILE_COUNT_LANDING"
          }
        }
      }
    }
}

job("03.countFilesInDestinationDirectory") {
    label('master')
	description()
	keepDependencies(false)
	parameters {
		stringParam("AUDIO_FILES_LANDING_DIRECTORY", "/mnt/public/data/landing", "")
		stringParam("AUDIO_FILES_DESTINATION_DIRECTORY", "/mnt/public/data/destination", "")
        choiceParam("SAD_THRESHOLD", [-45, -40, -35, -30, -25, -20, -15, -10, -5], "Threshold decible value")
		stringParam("FILE_COUNT_LANDING", "", "")
	}
	disabled(false)
	concurrentBuild(false)
	steps {
		shell("""#!/usr/bin/env bash +x

cd \${AUDIO_FILES_DESTINATION_DIRECTORY}

# Now print current working directory
pwd

fileCountDest=\$(find . -type f|wc -l)
echo "Total number of files in Destination directory: \${fileCountDest}"




if [ \${fileCountDest} -gt 0 ] 
then
    echo "FILE_COUNT_DEST=\${fileCountDest}" > /var/lib/jenkins/jobs/03.countFilesInDestinationDirectory/workspace/env.properties

	echo "############################################"
    echo "Files found now print file types"
    echo "********************************************"
    echo "********************************************"
    find . -type f | sed -e 's/.*\\.//' | sort | uniq -c | sort -n
    echo "********************************************"
    echo "********************************************"
else
    echo "############################################"
    echo "Exit: No files found in destination directory"
    echo "############################################"
	exit 1
fi""")
	
  
      downstreamParameterized {
        trigger('04.matchFilesInLandingAndDestinationDirectories') {
          
          block {
            buildStepFailure('FAILURE')
            failure('FAILURE')
            unstable('UNSTABLE')
          }
          
          parameters {
              currentBuild()
              propertiesFile('${WORKSPACE}/env.properties', true)
          }
        }
      }
    }
}

job("04.matchFilesInLandingAndDestinationDirectories") {
    label('master')
	description()
	keepDependencies(false)
	parameters {
		stringParam("AUDIO_FILES_LANDING_DIRECTORY", "/mnt/public/data/landing", "")
		stringParam("AUDIO_FILES_DESTINATION_DIRECTORY", "/mnt/public/data/destination", "")
        choiceParam("SAD_THRESHOLD", [-45, -40, -35, -30, -25, -20, -15, -10, -5], "Threshold decible value")
        stringParam("FILE_COUNT_LANDING", "", "")
        stringParam("FILE_COUNT_DEST", "", "")
      
	}
	disabled(false)
	concurrentBuild(false)
	steps {
		shell("""
if [ \$FILE_COUNT_LANDING != \$FILE_COUNT_DEST ]; then
	echo "Files count in Landing and Destination folders doesn't match" 
    exit 1
fi""")
	
  
      downstreamParameterized {
        trigger('05.TriggerSeedJob') {
          
          block {
            buildStepFailure('FAILURE')
            failure('FAILURE')
            unstable('UNSTABLE')
          }
          
          parameters {
              currentBuild()
          }
        }
      }
    }
}

job("05.TriggerSeedJob") {
    label('master')
	description()
	keepDependencies(false)
	parameters {
		stringParam("AUDIO_FILES_DESTINATION_DIRECTORY", "/mnt/public/data/destination", "")
        choiceParam("SAD_THRESHOLD", [-45, -40, -35, -30, -25, -20, -15, -10, -5], "Threshold decible value")
	}
	disabled(false)
	concurrentBuild(false)
	steps {
      shell("""

    ls -ltr \${AUDIO_FILES_DESTINATION_DIRECTORY}
    jobsCount=`ls \${AUDIO_FILES_DESTINATION_DIRECTORY}|wc -l`
    echo "NUMBER_OF_FOLDERS=\${jobsCount}" > /var/lib/jenkins/jobs/05.TriggerSeedJob/workspace/env.properties

""")
	
  
        downstreamParameterized {
          trigger('JobsSeed') {
            
//            block {
//              buildStepFailure('FAILURE')
//              failure('FAILURE')
//              unstable('UNSTABLE')
//            }
            
            parameters {
                currentBuild()
                propertiesFile('${WORKSPACE}/env.properties', true)
            }
          }
        }
    }
}
}

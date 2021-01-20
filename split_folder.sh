#!/bin/bash

source=$1

if [[ $# -ne 1 ]]; then
    echo "Please provide folder name."
    exit 2
fi

number_of_folders=2

second_folder_name=${source}_$(date +%Y%m%d%H%M%S)

cp -rf ${source} ${second_folder_name}

#i=0

#while [ $i -lt $number_of_folders ]
#do
#  i=$[$i+1]
#  cp -rf $source ${source}_part$i
#done

ls $source/ > ${source}_files_list.txt
total_files=$(wc -l < ${source}_files_list.txt)

if [ "$(($total_files%$number_of_folders))" == 1 ]; then
  echo "Odd"
  folder1_files="$((($total_files+1)/2))"
  folder2_files="$(($total_files-$folder1_files))"
else
  echo "even"
  folder1_files="$(($total_files/2))"
  folder2_files="$(($total_files-$folder1_files))"
fi

echo $total_files
echo $folder1_files
echo $folder2_files

head -n $folder1_files ${source}_files_list.txt > folder_1.txt
tail -n $folder2_files ${source}_files_list.txt > folder_2.txt

sed 's/^/rm -rf /g' folder_1.txt > $source/folder_1.sh
sed 's/^/rm -rf /g' folder_2.txt > ${second_folder_name}/folder_2.sh

echo "*********************"
cd $source
bash folder_1.sh
cd ..

echo "*********************"
cd ${second_folder_name}
bash folder_2.sh
cd ..

rm folder_1.txt folder_2.txt $source/folder_1.sh ${second_folder_name}/folder_2.sh

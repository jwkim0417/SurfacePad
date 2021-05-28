for input_file in `find . -name "*.pcm"`
do
  output_file=`echo $input_file | sed 's/\//-/g' | sed 's/\.-//g' | sed 's/\.pcm/\.wav/g'`
  ffmpeg -v quiet -f s16le -ar 48k -ac 2 -i $input_file $output_file
done
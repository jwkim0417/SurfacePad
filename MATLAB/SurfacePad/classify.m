k = knocks();
%%
fs = 48000;
precision = 'int16';
fid = fopen('calibrate1.pcm');
audio = double(fread(fid, Inf, precision, 'ieee-le'));
fclose(fid);

n_odd = audio(1:2:end) / 32768;
n_even = audio(2:2:end) / 32768;

data = [n_odd n_even];

%%
k = k.calibration(data);
%%
ret = k.perform(data);
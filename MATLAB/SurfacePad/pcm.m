
%%
fs = 48000;
precision = 'int16';
fid = fopen('2021-05-14-10_19_08.pcm');
audio = int16(fread(fid, Inf, precision, 'ieee-le'));
fclose(fid);
% stereo = reshape(audio, [], 2);

n_odd = audio(1:2:end);
n_even = audio(2:2:end);

stereo = [n_odd n_even];

audiowrite('test.wav', stereo, fs, 'BitsPerSample', 16);
%%
fid = fopen('2021-05-10-06_50_03.pcm');
audio = int16(fread(fid, Inf, precision, 'ieee-le'));
fclose(fid);
% stereo = reshape(audio, [], 2);

a2 = audio(1:2:end);
% n_odd = [a2(100:end); zeros(99,1)];
% n_even = [a2(50:end); zeros(49,1)];
n_odd = a2;
n_even = a2*2;

stereo = [n_odd n_even];

audiowrite('mono5.wav', stereo, fs, 'BitsPerSample', 16);
%%
play(player)
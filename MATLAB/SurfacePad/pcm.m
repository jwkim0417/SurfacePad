
%%
fs = 48000;
precision = 'int16';
fid = fopen('2021-05-27-07_33_45.pcm');
audio = int16(fread(fid, Inf, precision, 'ieee-le'));
fclose(fid);
% stereo = reshape(audio, [], 2);

n_odd = audio(1:2:end);
n_even = audio(2:2:end);

stereo = [n_odd n_even];
%%
figure;
plot(n_odd);

% audiowrite('test.wav', stereo, fs, 'BitsPerSample', 16);
%%
m_data = table2array(data);
m_data1 = table2array(data1);
figure;
subplot(2, 1, 1);
plot(m_data);
subplot(2, 1, 2);
plot(m_data1);
%%
for i = 1:256
    m_data(i) = data(i,1);
    m_data1(i) = data1(i,1);
end
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
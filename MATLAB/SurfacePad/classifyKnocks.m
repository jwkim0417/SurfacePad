disp('Classify Knocks')
mic1 = data(:, 1).^2;
mic2 = data(:, 2).^2;
[m1, i1] = max(mic1);
mic1_cut = mic1(i1-30:i1+120);
mic2_cut = mic2(i1-30:i1+120);
mic1_cut_n = mic1_cut / max(mic1_cut);
mic2_cut_n = mic2_cut / max(mic2_cut);
mic12_xc = xcorr(mic1_cut_n, mic2_cut_n);
% figure; plot(-150:150, mic12_xc);
% title('Corss-Correlation')
% xlabel('Frame Diff')
% ylabel('xcorr')
% grid on;

[mm, idx] = max(mic12_xc);
offset = 0;
idx = idx - 151;
if idx < -10
    disp('LEFT/UP')
elseif idx > 10
    disp('RIGHT/DOWN')
else
    disp('CENTER')
end
dist = idx * 340 / 48000 * 100;
disp([num2str(dist), 'cm, ', num2str(idx), 'samples'])
%%
x = 1:length(mic1_cut);
figure; plot(x, mic1_cut_n, x, mic2_cut_n);
%%
figure;
subplot(2, 1, 1);
plot(data(:,1));
ylim([-0.2 0.2]);
subplot(2, 1, 2);
plot(data(:,2));
ylim([-0.2 0.2]);
function offset = calibration(data)
%CALIBRATION 이 함수의 요약 설명 위치
%   자세한 설명 위치
    mic1 = data(:, 1).^2;
    mic2 = data(:, 2).^2;
    [m1, i1] = max(mic1);
    mic1_cut = mic1(i1-30:i1+120);
    mic2_cut = mic2(i1-30:i1+120);
    mic1_cut_n = mic1_cut / max(mic1_cut);
    mic2_cut_n = mic2_cut / max(mic2_cut);
    mic12_xc = xcorr(mic1_cut_n, mic2_cut_n);


end


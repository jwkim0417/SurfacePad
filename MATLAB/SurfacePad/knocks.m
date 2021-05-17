classdef knocks
    properties
        offset
        calibrateCount
    end
    
    methods
        function obj = knocks()
            obj.offset = 0;
            obj.calibrateCount = 0;
        end
        
        function obj = calibration(obj, data)
            mic1 = data(:, 1).^2;
            mic2 = data(:, 2).^2;
            [~, i1] = max(mic1);
            mic1_cut = mic1(i1-30:i1+120);
            mic2_cut = mic2(i1-30:i1+120);
            mic1_cut_n = mic1_cut / max(mic1_cut);
            mic2_cut_n = mic2_cut / max(mic2_cut);
            mic12_xc = xcorr(mic1_cut_n, mic2_cut_n);
            [~, idx] = max(mic12_xc);
            obj.calibrateCount = obj.calibrateCount + 1;
            obj.offset = (obj.offset + 151 - idx) / obj.calibrateCount;
        end
        
        function ret = perform(obj, data)
            mic1 = data(:, 1).^2;
            mic2 = data(:, 2).^2;
            [~, i1] = max(mic1);
            mic1_cut = mic1(i1-30:i1+120);
            mic2_cut = mic2(i1-30:i1+120);
            mic1_cut_n = mic1_cut / max(mic1_cut);
            mic2_cut_n = mic2_cut / max(mic2_cut);
            mic12_xc = xcorr(mic1_cut_n, mic2_cut_n);
            [~, idx] = max(mic12_xc);
            
            idx = idx - 151 + obj.offset;
            if idx < -7
                disp('LEFT/UP')
                ret = 'LEFT/UP';
            elseif idx > 7
                disp('RIGHT/DOWN')
                ret = 'RIGHT/DOWN';
            else
                disp('CENTER')
                ret = 'CENTER';
            end
            dist = idx * 340 / 48000 * 100;
            disp([num2str(dist), 'cm, ', num2str(idx), 'samples'])
        end
    end
end


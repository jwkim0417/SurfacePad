classdef Swipe
    %SWIPE �� Ŭ������ ��� ���� ��ġ
    %   �ڼ��� ���� ��ġ
    
    properties
        Property1
    end
    
    methods
        function obj = Swipe(inputArg1,inputArg2)
            %SWIPE �� Ŭ������ �ν��Ͻ� ����
            %   �ڼ��� ���� ��ġ
            obj.Property1 = inputArg1 + inputArg2;
        end
        
        function outputArg = method1(obj,inputArg)
            %METHOD1 �� �޼����� ��� ���� ��ġ
            %   �ڼ��� ���� ��ġ
            outputArg = obj.Property1 + inputArg;
        end
    end
end


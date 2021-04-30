figure(2);
subplot(2, 2, [1, 3]);
plot(data);
ylim([-1 1]);
subplot(2, 2, 2);
plot(data2(:, 1));
ylim([-1 1]);
subplot(2, 2, 4);
plot(data2(:, 2));
ylim([-1 1]);


plot(data2(:,1)-data2(:, 2))
ylim([-1 1]);

[a, b] = envelope(data2(:, 1), 3000, 'peak');
[a2, b2] = envelope(data2(:, 2), 3000, 'peak');

plot(a)
hold on;
plot(a2)

figure(100);
plot(a-a2);

s = data2(:, 1);
Y = fft(s);
L = length(s);

P2 = abs(Y/L);
P1 = P2(1:L/2+1);
P1(2:end-1) = 2*P1(2:end-1);
f = fs*(0:(L/2))/L;

figure(2);
subplot(1, 2, 1);
plot(f, P1);

s = data2(:, 2);
Y = fft(s);
L = length(s);

P2 = abs(Y/L);
P1 = P2(1:L/2+1);
P1(2:end-1) = 2*P1(2:end-1);
f = fs*(0:(L/2))/L;


subplot(1, 2, 2);
plot(f, P1);
xlabel('Frequency(Hz)');
ylabel('Magnitude');


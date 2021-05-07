figure(2);
subplot(2, 2, [1, 3]);
plot(data);
dt1 = data;
dt2 = [data(15:end); zeros(14,1)];
dt = [dt1, dt2];

figure(21);
subplot(2, 1, 1);
plot(dt(:, 1));
ylim([-0.13 0.13]);
subplot(2, 1, 2);
plot(dt(:, 2));
ylim([-0.13 0.13]);
%%
figure(31);
subplot(2, 1, 1);
plot(knock(:, 1));
ylim([-0.5 0.5]);
subplot(2, 1, 2);
plot(knock(:, 2));
ylim([-0.5 0.5]);
%%
figure(41);
plot(knock(:, 1), 'r');
hold on;
plot(knock(:, 2), 'b');
ylim([-0.5 0.5]);
hold off;
%%
figure(31);
subplot(2, 1, 1);
plot(ci(:, 1));
ylim([-0.5 0.5]);
subplot(2, 1, 2);
plot(ci(:, 2));
ylim([-0.5 0.5]);
%%
figure(71);
[a, b] = envelope(ci(:, 1), 5000, 'peak');
[a2, b2] = envelope(ci(:, 2), 5000, 'peak');

plot(a)
hold on;
plot(a2)
hold off;
%%
s = knock(:, 1);
Y = fft(s);
L = length(s);

P2 = abs(Y/L);
P1 = P2(1:L/2+1);
P1(2:end-1) = 2*P1(2:end-1);
f = fs*(0:(L/2))/L;

figure
plot(f, P1);
xlabel('Frequency(Hz)');
ylabel('Magnitude');

%% Diff
ylim([-1 1]);
subplot(2, 2, 2);
plot(data2(:, 1));
ylim([-0.2 0.2]);
subplot(2, 2, 4);
plot(data2(:, 2));
ylim([-0.2 0.2]);

figure(11);
subplot(2, 1, 1);
plot(drag(:, 1));
ylim([-0.13 0.13]);
subplot(2, 1, 2);
plot(drag(:, 2));
ylim([-0.13 0.13]);

figure(15);
subplot(2, 1, 1);
cccc = up_down(:, 1)./ up_down(:, 2);
plot(cccc)
plot(up_down(:, 1));
ylim([-1 1]);
subplot(2, 1, 2);
plot(up_down(:, 2));
ylim([-1 1]);

dif = data2(:, 1)-data2(:,2);
[d1, d2] = envelope(dif, 5000, 'peak');
figure(3);
plot(d1)
plot(data2(:,1)-data2(:, 2))
ylim([-0.15 0.15]);

[a, b] = envelope(data2(:, 1), 3000, 'peak');
[a2, b2] = envelope(data2(:, 2), 3000, 'peak');

plot(a)
hold on;
plot(a2)
hold off;

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


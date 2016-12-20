% This script loads and synchronyces data acquired with the acqPlatform
% package in android.

close all;
clear all;

datasetFolder = '/mnt/DATA/Datasets/androidDatasets/datasets';

collectionName = '2016-12-05_152331_samsung_SM-G925F_10steps';

imgres = '640x480';
imgext = '.jpg';


% Read the sensors log files
data_times = [];
sensors_read = [];
listOfLogFiles = dir(sprintf('%s/%s/sensor_*_log.csv',datasetFolder,collectionName));
for it_log = 1:length(listOfLogFiles)
    datatype = listOfLogFiles(it_log).name(8:end-8);
    datatable = readtable(...
        sprintf('%s/%s/%s',datasetFolder,collectionName,listOfLogFiles(it_log).name),...
        'ReadVariableNames',false,...
        'HeaderLines',1);
    if (~isempty(datatable))
        switch datatype
            % PHONE SENSORS
            case 'ACCELEROMETER'
                accelerometer.datatype = datatype;
                accelerometer.sysTime = table2array(datatable(:,1));
                accelerometer.evntTime = table2array(datatable(:,2));
                data_times = [data_times; accelerometer.evntTime];
                accelerometer.value = table2array(datatable(:,3:end));
                sensors_read = [sensors_read; accelerometer];
                
            case 'LINEAR_ACCELERATION'
                linaccelerometer.datatype = datatype;
                linaccelerometer.sysTime = table2array(datatable(:,1));
                linaccelerometer.evntTime = table2array(datatable(:,2));
                data_times = [data_times; linaccelerometer.evntTime];
                linaccelerometer.value = table2array(datatable(:,3:end));
                sensors_read = [sensors_read; linaccelerometer];
                
            case 'GRAVITY'
                gravity.datatype = datatype;
                gravity.sysTime = table2array(datatable(:,1));
                gravity.evntTime = table2array(datatable(:,2));
                data_times = [data_times; gravity.evntTime];
                gravity.value = table2array(datatable(:,3:end));
                sensors_read = [sensors_read; gravity];
                
            case 'GYROSCOPE'
                gyroscope.datatype = datatype;
                gyroscope.sysTime = table2array(datatable(:,1));
                gyroscope.evntTime = table2array(datatable(:,2));
                data_times = [data_times; gyroscope.evntTime];
                gyroscope.value = table2array(datatable(:,3:end));
                sensors_read = [sensors_read; gyroscope];
                
            case 'GAME_ROTATION_VECTOR'
                gamerotationvector.datatype = datatype;
                gamerotationvector.sysTime = table2array(datatable(:,1));
                gamerotationvector.evntTime = table2array(datatable(:,2));
                data_times = [data_times; gamerotationvector.evntTime];
                gamerotationvector.value = table2array(datatable(:,3:end));
                sensors_read = [sensors_read; gamerotationvector];
                
            case 'ROTATION_VECTOR'
                rotationvector.datatype = datatype;
                rotationvector.sysTime = table2array(datatable(:,1));
                rotationvector.evntTime = table2array(datatable(:,2));
                data_times = [data_times; rotationvector.evntTime];
                rotationvector.value = table2array(datatable(:,3:end));
                sensors_read = [sensors_read; rotationvector];
                
                % CPRO R EXTERTAL SENSOR
            case 'CPRO_R_ACCELEROMETER'
                cR_accelerometer.datatype = datatype;
                cR_accelerometer.sysTime = table2array(datatable(:,1));
                cR_accelerometer.evntTime = table2array(datatable(:,2));
                data_times = [data_times; cR_accelerometer.sysTime];
                cR_accelerometer.value = table2array(datatable(:,3:end));
                sensors_read = [sensors_read; cR_accelerometer];
                
            case 'CPRO_R_BAROMETER'
                cR_barometer.datatype = datatype;
                cR_barometer.sysTime = table2array(datatable(:,1));
                cR_barometer.evntTime = table2array(datatable(:,2));
                data_times = [data_times; cR_barometer.sysTime];
                cR_barometer.value = table2array(datatable(:,3:end));
                sensors_read = [sensors_read; cR_barometer];
                
            case 'CPRO_R_GYROSCOPE'
                cR_gyroscope.datatype = datatype;
                cR_gyroscope.sysTime = table2array(datatable(:,1));
                cR_gyroscope.evntTime = table2array(datatable(:,2));
                data_times = [data_times; cR_gyroscope.sysTime];
                cR_gyroscope.value = table2array(datatable(:,3:end));
                sensors_read = [sensors_read; cR_gyroscope];
                
            case 'CPRO_R_STEPS'
                cR_steps.datatype = datatype;
                cR_steps.sysTime = table2array(datatable(:,1));
                cR_steps.evntTime = table2array(datatable(:,2));
                data_times = [data_times; cR_steps.sysTime];
                cR_steps.value = table2array(datatable(:,3:end));
                sensors_read = [sensors_read; cR_steps];
                
                % CPRO R EXTERTAL SENSOR
            case 'CPRO_L_ACCELEROMETER'
                cL_accelerometer.datatype = datatype;
                cL_accelerometer.sysTime = table2array(datatable(:,1));
                cL_accelerometer.evntTime = table2array(datatable(:,2));
                data_times = [data_times; cL_accelerometer.sysTime];
                cL_accelerometer.value = table2array(datatable(:,3:end));
                sensors_read = [sensors_read; cL_accelerometer];
                
            case 'CPRO_L_BAROMETER'
                cL_barometer.datatype = datatype;
                cL_barometer.sysTime = table2array(datatable(:,1));
                cL_barometer.evntTime = table2array(datatable(:,2));
                data_times = [data_times; cL_barometer.sysTime];
                cL_barometer.value = table2array(datatable(:,3:end));
                sensors_read = [sensors_read; cL_barometer];
                
            case 'CPRO_L_GYROSCOPE'
                cL_gyroscope.datatype = datatype;
                cL_gyroscope.sysTime = table2array(datatable(:,1));
                cL_gyroscope.evntTime = table2array(datatable(:,2));
                data_times = [data_times; cL_gyroscope.sysTime];
                cL_gyroscope.value = table2array(datatable(:,3:end));
                sensors_read = [sensors_read; cL_gyroscope];
                
            case 'CPRO_L_STEPS'
                cL_steps.datatype = datatype;
                cL_steps.sysTime = table2array(datatable(:,1));
                cL_steps.evntTime = table2array(datatable(:,2));
                data_times = [data_times; cL_steps.sysTime];
                cL_steps.value = table2array(datatable(:,3:end));
                sensors_read = [sensors_read; cL_steps];
                
                % CAMERA
            case 'CAMERA'
                camera.datatype = datatype;
                camera.sysTime = table2array(datatable(:,1));
                camera.evntTime = table2array(datatable(:,2));
                data_times = [data_times; camera.evntTime];
                camera.value = table2array(datatable(:,3:end));
                sensors_read = [sensors_read; camera];
        end
    end
end

% Read images
listOfImg = dir(sprintf('%s/%s/images_%s/*%s',datasetFolder,...
    collectionName, imgres, imgext));
imgTime = zeros(size(listOfImg));
for it_img = 1:length(listOfImg)
    index = [find(listOfImg(it_img).name == '_') ...
        find(listOfImg(it_img).name == '.')];
    imgTime(it_img) = (1/1000000000)*(str2double(...
        listOfImg(it_img).name(index(1)+1:index(2)-1)));
end

% Acquisition times
min_sys_time = min(data_times);
max_sys_time = max(data_times);
sys_time_val = unique(data_times);

% Print information about the data acquired
fprintf('\nData collection: %s\n',collectionName);
fprintf('Data time: %2.2f [secs]\n',(1/1000000000)*(max_sys_time-min_sys_time));
for it_sensor = 1:length(sensors_read)
    fprintf('\n  %20s: %6d reads (%d values), %3.3f Hz\n',...
        sensors_read(it_sensor).datatype,...
        length(sensors_read(it_sensor).sysTime),...
        size(sensors_read(it_sensor).value,2),...
        length(sensors_read(it_sensor).sysTime)/...
        ((1/1000000000)*(sensors_read(it_sensor).sysTime(end)-sensors_read(it_sensor).sysTime(1))));
    if strcmp(sensors_read(it_sensor).datatype, 'CAMERA')
        img = imread(sprintf('%s/%s/images_%s/%s',datasetFolder,...
            collectionName,imgres,listOfImg(it_img).name));
        fprintf('                        %6d x %4d %1d ch\n',size(img,2),size(img,1),...
            size(img,3))
    end
    
end
img = imread(sprintf('%s/%s/images_%s/%s',datasetFolder,...
    collectionName,imgres,listOfImg(it_img).name));
fprintf('\n                IMAGES: %6d images, %3.3f Hz\n',length(listOfImg),...
    length(imgTime)/(imgTime(end)-imgTime(1)));
fprintf('                        %6d x %4d %1d ch\n',size(img,2),size(img,1),...
    size(img,3));
fprintf('For more info check the session_description.txt file\n');

% Events in time and synchronization
fig = figure;
hold on;
grid on;
axis([-0.1 max_sys_time-min_sys_time 0 length(sensors_read)+1]);
for it_sensor = 1:length(sensors_read)
    datatype = sensors_read(it_sensor).datatype;
    if strcmp(datatype(1:4),'CPRO')
        toplot_x = sensors_read(it_sensor).sysTime - min_sys_time;
        toplot_y = it_sensor * (ones(size(toplot_x)));
    else
        toplot_x = sensors_read(it_sensor).evntTime - min_sys_time;
        toplot_y = it_sensor * (ones(size(toplot_x)));
    end
    plot(toplot_x, toplot_y,'.');
    text(0, it_sensor + 0.25, datatype, 'Interpreter', 'none');
end

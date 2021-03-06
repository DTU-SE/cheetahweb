angular.module('cheetah.UploadData', ['ngRoute', 'ui.select'])
    .controller('UplaodDataCtrl', function ($scope, $http) {
        $scope.uploadedFiles = [];
        $scope.filesToUpload = 0;
        $scope.uploadPercentage = 100;
        $scope.unmappedFiles = [];
        $scope.subjects = [];
        var dropZone = document.getElementById('drop-zone');
        var uploadForm = document.getElementById('js-upload-form');

        function updateProgress() {
            $scope.uploadPercentage = $scope.uploadedFiles.length / $scope.filesToUpload * 100;
            if ($scope.uploadPercentage < 100) {
                $("#uploadprogressbar").addClass("active");
            } else {
                $("#uploadprogressbar").removeClass("active");
            }
        }

        $scope.setSelection = function (file, event) {
            //prevent multiple calls when clicking on check box
            var target = event.target;
            if (target.type === 'checkbox') {
                return;
            }
            file.selection = !file.selection;
        };

        function uploadQueuedeFiles(fileQueueToSend) {
            if(fileQueueToSend.length>0) {
                var itemToSend =fileQueueToSend.pop();
                $http.post('../../private/uploaddata', itemToSend.formData, {
                    withCredentials: true,
                    headers: {'Content-Type': undefined},
                    transformRequest: angular.identity,
                    filename: itemToSend.name
                }).success(function (data, status, headers, config) {
                        var status = {
                            fileId: data.id, filename: config.filename, status: "success"
                        };
                        $scope.uploadedFiles.push(status);
                        updateProgress();
                        uploadQueuedeFiles(fileQueueToSend);
                    }
                ).error(
                    function (data, status, headers, config) {
                        var statusMessage = "failed";

                        //could not map file to user
                        if (status === 418) {
                            statusMessage = "Unable to find subject";
                            $scope.unmappedFiles.push(data);
                            if (!($("#cheetah-map-files-to-subjects-dialog").data('bs.modal') || {isShown: false}).isShown) {
                                $http.get('../../private/listSubjects').success(function (data) {
                                    data.unshift({id: -1, subjectId: "No subject selected", study: {name: "No study"}});
                                    $scope.subjects = data;
                                    $("#cheetah-map-files-to-subjects-dialog").modal('show');
                                });
                            }
                        }

                        var status = {
                            fileId: data.id, filename: config.filename, status: statusMessage
                        };
                        $scope.uploadedFiles.push(status);
                        updateProgress();
                        uploadQueuedeFiles(fileQueueToSend);
                    }
                );
            }
        }

        var startUpload = function (files) {
            var fileQueueToSend=[];
            for (var i = 0; i < files.length; i++) {
                var itemToSend={};
                var formData = new FormData();
                formData.append("pupilData", files[i]);
                $scope.filesToUpload++;
                itemToSend.formData=formData;
                itemToSend.name=files[i].name;
                fileQueueToSend.push(itemToSend);
                updateProgress();
            }
            uploadQueuedeFiles(fileQueueToSend);
        };

        $scope.mapFilesToSubject = function () {
            var toMap = {};
            var indexToRemove = [];

            $.each($scope.unmappedFiles, function (index, file) {
                var fileId = file.id;
                var value = file.subject.id;
                if (value > 0) {
                    toMap[fileId] = value;
                    indexToRemove.push(index);

                    $.each($scope.uploadedFiles, function (index, uploadedFile) {
                        if (uploadedFile.fileId === fileId) {
                            uploadedFile.status = "success";
                            return false;
                        }
                    });
                }
            });

            for (var i = indexToRemove.length - 1; i >= 0; i--) {
                var toRemove = indexToRemove[i];
                $scope.unmappedFiles.splice(toRemove, 1);
            }

            $http.post('../../private/mapFilesToSubject', {filesToSubjectIds: toMap});

            if ($scope.unmappedFiles.length === 0) {
                $("#cheetah-map-files-to-subjects-dialog").modal('hide');
            }
        };

        uploadForm.addEventListener('submit', function (e) {
            var uploadFiles = document.getElementById('js-upload-files').files;
            e.preventDefault();

            startUpload(uploadFiles)
        });

        dropZone.ondrop = function (e) {
            e.preventDefault();
            this.className = 'upload-drop-zone';

            startUpload(e.dataTransfer.files)
        };

        dropZone.ondragover = function () {
            this.className = 'upload-drop-zone drop';
            return false;
        };

        dropZone.ondragleave = function () {
            this.className = 'upload-drop-zone';
            return false;
        };

    }).config(function ($routeProvider) {
    $routeProvider
        .when('/', {
            templateUrl: 'angular/uploaddata/uploaddata.htm',
            controller: 'UplaodDataCtrl'
        });
});
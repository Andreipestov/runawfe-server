wfe.processDefs = new function() {
    var app = null;

    this.onLoad = function() {
        this.onUnload();  // just in case
        wfe.ajaxGetJsonAndReady("processDefs", {}, function(data) {
            app = new Vue({
                el: "#spa-body",
                data: data
            });
            var c_height = $(window).height()-180;
			$(".one-contentback").css('minHeight', c_height);
			$(".two-contentback").css('minHeight', c_height);
            $(".process-name").click(function() {
                var data = {
                    processId: getProcessId($(this).text(), app.rows)
                };

                wfe.ajaxGetJsonAndReady("processForm", data, function(processData) {
                    if (!$("#two-contentback").hasClass("visibility-toggle")) {
                        $("#one-contentback").toggleClass("two-c30");
                        $("#two-contentback").toggleClass("visibility-toggle");
                    }

                    $(".long-description").hide();

                    $(".process-name-caption")[0].innerHTML = processData.rows[0].name;
                    $(".firstform")[0].innerHTML = processData.rows[0].form;
                    $(".firstform").show();

                    $(".input-buttons").show();
                });
            });
            $(".desc-ling-open").click(function() {
                if (!$("#two-contentback").hasClass("visibility-toggle")) {
                    $("#one-contentback").toggleClass("two-c30");
                    $("#two-contentback").toggleClass("visibility-toggle");
                }

                $(".process-name-caption")[0].innerHTML = "";
                $(".long-description").show();
                $(".firstform").hide();
                $(".input-buttons").hide();
            });
        });
    };

    this.onUnload = function() {
        if (app) {
            app.$destroy();
            app = null;
        }
    };

    function getProcessId(processName, processes) {
        for (var i = 0 ; i < processes.length ; i++) {
            var process = processes[i];

            if (process.name === processName) {
                return process.id;
            }
        }
    }
};

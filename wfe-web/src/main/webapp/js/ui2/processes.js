wfe.processes = new function() {
    var app = null;
     this.onLoad = function() {
        this.onUnload();  // just in case
        wfe.ajaxGetJsonAndReady("processes", {}, function(data) {
            app = new Vue({
                el: "#spa-body",
                data: data
            });
            var c_height = $(window).height()-180;
			$(".one-contentback").css('minHeight', c_height);
			$(".task tr:nth-child(2) td p").append("<img src='img/ico/top-tun.png' class='filter-ico' />");
            $("#but-filter").click(function() {
                $("#view").hide();
                $("#info").hide();
                $(".filter").toggleClass("vert-mid");
                $(".filter-ico").toggle();
                $(".task tr:nth-child(1)").toggleClass("light1");
                $(".task tr:nth-child(3)").toggleClass("light1");
                $(".task tr:nth-child(2) td").toggleClass("light1 vert-line");
                if(c_height < oc_height){$(".filter").hasClass("vert-mid") ? $(".one-contentback").height($(".one-contentback").height()+130): $(".one-contentback").height($(".one-contentback").height()-130);}
            })
			$(".close-ti").click(function() {
                $("#view").hide();
				$("#info").hide();
            })
            $("#but-view").click(function() {
               $("#view").toggle();
               $("#info").css("display","none");
            })
            $("#but-info").click(function() {
               $("#info").toggle();
               $("#view").css("display","none");
            })
        });
    };
     this.onUnload = function() {
        if (app) {
            app.$destroy();
            app = null;
        }
    };
};

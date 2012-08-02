$(document).ready(function() {
	$('img.rpmInfo').cluetip({activation: 'click', sticky: true, closePosition: 'title', arrows: true, width: 750});
	$('img.info').cluetip({activation: 'click', sticky: true, closePosition: 'title', arrows: true, width: 500, ajaxCache: false});
});

window.yum = {
    saveRepo : function(reponame) {
        var reposelect = $('#reposelect');
        var destination = reposelect.val();

        $.ajax({
            type : 'POST',
            cache : false,
            url : '',
            data : 'name=' + reponame + '&destination=' + destination,
            success: function () {
                $('#reposelect *').removeAttr('selected');
                $('#reposelect [value= "' + destination + '"]').attr('selected','selected');
                alert('Saved');
            },
            error: function (xhr, status, error) {
                alert('Saving failed : ' + status);
            }
        });
    },
		
    selectRepo : function() {
        $('#cluetip a.button').show();
    },

    addTag : function(tagElem, reponame, tag) {
      $.ajax({
            type : 'POST',
            cache : false,
            url : reponame + '/tags/',
            data : 'tag=' + encodeURI(tag),
            success: function () {
                alert('Saved');
                window.location.reload();
            },
            error: function (xhr, status, error) {
                alert('Tagging failed : ' + status);
            }
        });
    },



    removeTag : function(tagElem, reponame, tag) {
        $.ajax({
            type : 'DELETE',
            cache : false,
            url : reponame + '/tags/' + encodeURI(tag),
            success: function () {
                alert('Saved');
                window.location.reload();
            },
            error: function (xhr, status, error) {
                alert('Tagging failed : ' + status);
            }
        });
    },

    addCustomTag : function(tagElem, reponame) {
        var input = $(tagElem.parentNode).children('input');
        var tag = $.trim(input.val());
        if (!tag.match(/^[\w\.\-]+$/)) {
            input.addClass('error');
            return;
        }

        input.removeClass('error');
        yum.addTag(tagElem, reponame, tag);
    }
}
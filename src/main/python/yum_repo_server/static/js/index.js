$(document).ready(function() {
	$('img.rpmInfo').cluetip({activation: 'click', sticky: true, closePosition: 'title', arrows: true, width: 750});
	$('img.virtualInfo').cluetip({activation: 'click', sticky: true, closePosition: 'title', arrows: true, width: 500, ajaxCache: false});
});

window.yum = {
		saveRepo : function(reponame) {
			reposelect = $('#reposelect');
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
		}
}
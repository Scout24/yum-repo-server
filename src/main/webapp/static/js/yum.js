$(function() {
  $('img.rpmInfo').cluetip({
    activation: 'click',
    sticky: true,
    closePosition: 'title',
    arrows: true,
    width: 750
  });

  $('img.repoInfo').cluetip({
    activation: 'click',
    sticky: true,
    closePosition: 'title',
    arrows: true,
    width: 750,
    ajaxCache: false,
    onShow: function (ct, ci) {
      window.yum.repoName = $('#yumRepoName').attr("name");
      yum.renderScheduledSwitch();
      yum.renderTags();
      yum.renderRpmsSlider();
    }
  });

  $('img.virtualRepoInfo').cluetip({
    activation: 'click',
    sticky: true,
    closePosition: 'title',
    arrows: true,
    width: 750,
    ajaxCache: false,
    onActivate: function () {
      if (!yum.repos) {
        yum.loadStaticRepos();
      }
    },
    onShow: function (ct, ci) {
      $('#externalSwitch').switchbutton().change(function () {
        if ($(this).prop('checked')) {
          $('#targetRepos').hide();
          $('#targetUrl').show();
        } else {
          $('#targetUrl').hide();
          $('#targetRepos').show();
        }
        $('#saveButton').show();
      });
      var targetRepos = $('#targetRepos');
      var targetSelect = targetRepos.get(0);
      var currentTarget = $('#currentTarget').val();
      for (var i = 0; i < yum.repos.length; i++) {
        var entry = yum.repos[i];
        var option = new Option(entry.name, 'static/' + entry.name);
        if (entry.name == currentTarget) {
          option.selected = true;
        }
        targetSelect.options[i] = option;
      }
    }
  });
});

window.yum = {
  setStaticRepoType : function(reponame, type) {
    yum.setRepoProperty(reponame, 'type', type);
  },

  setMaxKeepRpms : function(reponame, value) {
    yum.setRepoProperty(reponame, 'maxKeepRpms', value);
  },

  setRepoProperty : function(reponame, property, value) {
    $.ajax({
      type : 'PUT',
      cache : false,
      url : reponame + '/' + property,
      contentType: 'application/json',
      data : JSON.stringify(value),
      success: function () {
      },
      error: function (xhr, status, error) {
        alert('Saving failed : ' + status);
      }
    });
  },

  loadStaticRepos : function() {
    $.ajax('/repo/', {
      dataType: 'json',
      async : false,
      success: function (data, status, xhr) {
        yum.repos = data.items;
      }
    });
  },
  
  renderScheduledSwitch: function() {
      $('.scheduledSwitch').switchbutton().change(function () {
          var value = $(this).prop("checked") ? 'SCHEDULED' : 'STATIC';
          var reponame = $(this).attr("name");
          yum.setStaticRepoType(reponame, value);
        });
  },

  renderRpmsSlider: function() {
      var keepRpmsSlider = $('#maxKeepRpmsSlider');
      var keepRpmsValue = $('#maxKeepRpmsValue');
      keepRpmsSlider.slider({
        min: 0,
        max: 10,
        value: keepRpmsValue.text(),
        slide: function( event, ui ) {
          keepRpmsValue.text(ui.value);
          yum.setMaxKeepRpms(keepRpmsValue.attr('name'), ui.value);
        }
      });
  },
  
  renderTags: function() {
	  var tags = [];
	  $(".tag").each(function() {
		  tags.push({name: $(this).text()});
	  });
      $("#tagsInput").tokenInput([], {
			allowCreation: true,
			createTokenText: "Create new tag", 
			onAdd: function(value) {},
			onDelete: function(value) {},
			theme: "facebook",
			prePopulate: tags,
			hintText: "add a tag"
      });
  },
  
  resetTags: function() {
	var tags = $("#tagsInput").tokenInput("get");
    yum.deleteAllTags();
    yum.saveTags(tags);
	window.location.reload();
  },
  
  deleteAllTags: function() {
	$.ajax({
	    type : 'DELETE',
	    async: false,
	    cache : false,
	    url : yum.repoName + '/tags',
	    error: function (xhr, status, error) {
	      alert('Resetting tags failed : ' + status);
	    }
	});
  },

  deleteRepoTag : function(repoName, tagToDelete) {
    $.ajax({
        type : 'POST',
        cache : false,
        url : repoName + '/tags/' + tagToDelete,
        success: function () {
          yum.removeTag(tagToDelete);
        },
        error: function (xhr, status, error) {
          alert('Failed to remove tag: ' + status);
        }
    });
  },

  saveTags : function(tags) {
	$(tags).each(function() {
		var newTag = this.name;
		$.ajax({
		    type : 'POST',
		    async: false,
		    cache : false,
		    url : yum.repoName + '/tags',
		    data : {tag: newTag},
		    success: function () {
		      // spinner.hide();
		      //yum.displayNewTag(newTag);
		    },
		    error: function (xhr, status, error) {
		      // spinner.hide();
		      alert('Saving failed : ' + status);
		      // button.show();
		    }
		});
	});
  },
  
  saveVirtualRepo : function(name) {
    var external = $('#externalSwitch').prop('checked');
    var target;
    if (external) {
      target = $('#targetUrl').val();
    } else {
      target = $('#targetRepos').val();
    }

    $('#saveButton').hide();
    $('#saveSpinner').show();

    $.ajax({
      type : 'POST',
      cache : false,
      url : '',
      data : {name : name, destination: target},
      success: function () {
        $('#saveSpinner').hide();
      },
      error: function (xhr, status, error) {
        $('#saveSpinner').hide();
        alert('Saving failed : ' + status);
        $('#saveButton').show();
      }
    });
  }
};
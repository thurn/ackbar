$(function() {
  var PAGE_SIZE = 5;
  var fbCommentUpdate = function() {
    $("fb\\:comments").each(function(idx, el) {
      if (idx < PAGE_SIZE) return;
      FB.XFBML._processElement(el, { 
        localName: 'comments', 
        className: 'FB.XFBML.Comments'
      }, function() {
          FB.Event.fire('xfbml.render');
      });
    });
  }
  $('.discuss-link').live('click', function() {
      $(this).parent().find('.discuss-content').show('slow');
      $(this).removeClass('discuss-link');
  });
  $('#main').infinitescroll({
    navSelector  : "#page-nav",            
    nextSelector : "#next-page",
    itemSelector : "#main div.post",
    loadingImg: "/loading.gif",
    donetext: ""
  }, function() {
    fbCommentUpdate();
    $.getScript("http://platform.twitter.com/widgets.js");
  });
  fbCommentUpdate();
  if ($.browser.msie) {
    $(".discuss").remove();
  }
});

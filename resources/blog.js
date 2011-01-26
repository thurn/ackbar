head.ready(function() {

  $('.discuss').each(function(_, element) {
    $(element).delegate('.discuss-link', 'click', function() {
      $(element).find('.discuss-content').show('slow');
      $(element).find('.discuss-link').removeClass('discuss-link');
    });
  });

  /* Infinite Scroll */
  $('#main').infinitescroll({
    navSelector  : "#page-nav",            
    nextSelector : "#next-page",
    itemSelector : "#main div.post",
    loadingImg: "/loading.gif",
    donetext: ""
  });
});

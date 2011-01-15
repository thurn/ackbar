head.ready(function() {
$('#main').infinitescroll({
  navSelector  : "#page-nav",            
  nextSelector : "#next-page",
  itemSelector : "#main div.post",
  loadingImg: "/loading.gif",
  donetext: ""
});
});

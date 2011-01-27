$(document).ready(function() {
  $(document.body).height($(window).height() - 20);
  page_height = $('#frame').innerHeight() - 50;
  var page_offset = 0;
  var page_count = 0;
  var pages = [];
  var page_offsets = [0];
  $('.page').children().each(function(i, elem) {
    var offset = $(elem).offset().top;
    if( offset - page_offset > page_height ) {
      page_count += 1;
      page_offset = offset;
      pages[page_count] = [];
      page_offsets.push(offset);
    }
    if( page_count > 0 ) {
      pages[page_count].push(elem);
    }
  });
  $.each(pages, function(i, page_elems) {
    if( i > 0 ) {
      //console.log($(page_elems[0]).offset().top);
      var page = $(document.createElement('div'));
      page.addClass('page');
      //page.css('top', -page_offsets[1]*i);
      //page.css('left', (i * 35) + "em");
      $('#frame').append(page);
      $.each(page_elems, function(j, elem) {
        page.append(elem);
      });
    }
  });
  $('#frame').cycle({
    fx:     'scrollHorz',
    speed:  'fast',
    next:   '#frame',
    timeout: 0
  });
  $(document).keydown(function(e) {
    console.log($('#frame'));
    if(e.which == 37) {
      $('#frame').cycle('prev');
    }
    if(e.which == 39) {
      $('#frame').cycle('next');
    }
  });
});

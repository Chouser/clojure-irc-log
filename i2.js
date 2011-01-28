$(document).ready(function() {
  $(document.body).height($(window).height() - 20);
  $(document.body).css('overflow', 'hidden');
  var page_height = $('#frame').innerHeight() + 5;
  var prev_page = $('.page');

      var new_page = $(document.createElement('div'));

      new_page.insertAfter(prev_page);

  $('.page').children().each(function(i, elem) {
    var elem_height = $(elem).outerHeight();
    new_page.append(elem);
    $(elem).addClass('h'+elem_height+'pn'+($('.page').length)+'ph'+new_page.height());
    if(new_page.height() > page_height) {
      new_page.addClass('page');

      prev_page = new_page;
      new_page = $(document.createElement('div'));

      new_page.insertAfter(prev_page);
      new_page.append(prev_page.children('h1').clone());
      new_page.append(prev_page.children('h2').clone());
      new_page.append(elem); // move it again
    }
  });

  /*
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
  */
  $('#frame').cycle({
    fx:     'shuffle',
    speed:  'fast',
    next:   '#frame',
    timeout: 0
  });
  $(document).keydown(function(e) {
    console.log($('#frame'));
    if(e.which == 39 || e.which == 32) {
      $('#frame').cycle('next');
    }
    if(e.which == 37) {
      $('#frame').cycle('prev');
    }
  });
});

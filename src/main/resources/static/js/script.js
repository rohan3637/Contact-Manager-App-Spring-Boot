console.log("This is custom javascript");

const toggleSidebar = () => {
    if ($(".sidebar").is(":visible")) {
        $(".sidebar").css("display", "none");
        $(".content").css("margin-left", "2%");
    } else {
        $(".sidebar").css("display", "block");
        $(".content").css("margin-left", "20%");
    }
}

const search = () => {
    console.log("Searching......");
    let query = $("#search-input").val();
    if (query === "") {
        $(".search-result").hide();
    } else {
        console.log(query);
        let url = window.location.protocol + "//" + window.location.host + `/search/${query}`;
        fetch(url).then((response) => {
            return response.json();
        }).then((data) => {
            console.log(data);
            let text = `<div class='list-group'>`;
            data.forEach((contact) => {
                text += `<a href='/user/${contact.cid}/contact' style="background: whitesmoke" class='list-group-item list-group-item-action'>${contact.name}</a>`;
            });
            text += `</div>`
            $(".search-result").html(text).show();
        });
    }
}
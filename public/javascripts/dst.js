;(function(window, document) {

    // SET VISIBILITY OF JS ONLY ELEMENTS
    (function() {
        const jsOnlyEls = document.getElementsByClassName('jsonly')
        Array.prototype.map.call(jsOnlyEls, el => el.style.display = 'block')
    })();

    function printElement(elId, ms) {
        var divToPrint = document.getElementById(elId);
        newWin = window.open();
        newWin.document.write(divToPrint.innerHTML);
        newWin.focus();
        setTimeout(function() {printNewWindow()}, ms);
    }
    function printNewWindow(){
        newWin.print();
        newWin.close();
    };

    var showHideContent = new GOVUK.ShowHideContent()
    showHideContent.init()
    
    GOVUK.shimLinksWithButtonRole.init()

    // Error summary focus
    function setUpErrorSummary() {
        var errorSummary = document.querySelector('.error-summary');
        if (errorSummary) {
          errorSummary.focus();
        }
    }
    setUpErrorSummary()

    var countryEl = document.querySelector("#location-autocomplete");
    if (countryEl) {
        openregisterLocationPicker({
            selectElement: countryEl,
            name: 'countryCode-name',
            url: '/digital-services-tax/assets/location-autocomplete-graph.json',
            defaultValue: ''
        });

        var wrapper = document.querySelector('.country-code-wrapper');
        // TODO - not entirely sure this is desired behaviour so not implementing - nb this file is not included in govuk_wrapper
        function resetSelectIfEmpty(e) {
            if (e.target.id === 'countryCode') {
                var val = e.target.value.trim();
                var countrySelect = document.querySelector("#countryCode-select");
                if (countrySelect) {
                    var countriesArray = Array.prototype.slice.call(countrySelect.options);
                    var matches = countriesArray.filter(function (o) {
                        return o.text !== '' && o.text === val
                    });
                    if (!matches.length) {
                        countrySelect.value = ''
                    }
                }
            }
        }

        wrapper.addEventListener('change', resetSelectIfEmpty);
        
    }
})(window, document);
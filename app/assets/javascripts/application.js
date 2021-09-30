window.GOVUKFrontend.initAll();
window.HMRCFrontend.initAll();

;(function(window, document) {
    var countryEl = document.querySelector("#location-autocomplete");
    if (countryEl) {
        openregisterLocationPicker({
            selectElement: countryEl,
            name: 'countryCode-name',
            url: '/digital-services-tax/assets/location-autocomplete-graph.json',
            defaultValue: ''
        });

        var wrapper = document.querySelector('.country-code-wrapper');
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
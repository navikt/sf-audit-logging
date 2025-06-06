document.addEventListener("DOMContentLoaded", function () {
    const loadingSpinner = document.getElementById("loading");
    const metadataContainer = document.getElementById("metadata-container");

    // Show loading spinner
    loadingSpinner.style.display = "block";

    const projectTitleElement = document.getElementById("project-title");

    fetch('/internal/guiLabel')
        .then(response => response.text()) // Resolve the text from the response
        .then(context => {
            projectTitleElement.innerText = "Salesforce Audit Log " + context;
        })
        .catch(error => {
            console.error("Error fetching context:", error);
        });
    // Fetch metadata
    fetch('/internal/metadata')
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            loadingSpinner.style.display = "none"; // Hide loading spinner
            renderMetadata(data); // Render metadata
            //highlightUnmappedFields();
        })
        .catch(error => {
            loadingSpinner.style.display = "none"; // Hide loading spinner
            metadataContainer.innerHTML = `<p style="color:red;">Failed to load metadata: ${error.message}</p>`;
        });

    function renderMetadata(metadata) {
        metadataContainer.innerHTML = `
        <table class="table-columns" border="1" style="border-collapse: collapse; margin: 0 auto;">
            <thead>
                <tr>
                    <th>Sync Date</th>
                    <th>Event Date</th>
                    <th>Object</th>
                    <th>Number of Records</th>
                </tr>
            </thead>
        <tbody>
    ${Object.entries(metadata).flatMap(([syncDate, records]) =>
            records.map(record => `
            <tr>
                <td>${syncDate}</td>
                <td>${record.eventDate}</td>
                <td>${record.entity}</td>
                <td>${record.numberOfRecords}</td>
            </tr>
        `).join('')
        ).join('')}
            </tbody>
        </table>
    `;
    }
})

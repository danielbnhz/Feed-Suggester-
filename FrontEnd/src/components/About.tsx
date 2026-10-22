export default function About() {
    return (
        <section className="about" aria-label="About">
            <h2>About Feed-Suggester</h2>
            <p>
                A scrappy, no-ads RSS reader that scans a wide set of sources and
                surfaces a live feed of articles worth reading. It's built to run on
                the cheapest hosting there is.
            </p>
            <h3>Why I built it</h3>
            <p>
                I wanted a feed I control that doesn't die or fill up with sponsored
                junk.
            </p>
            <h3>About me</h3>
            <p>
               I love software, and have all my life. This is an attempt i am making to create a handy
                little feed aggregator.
            </p>
            <p>
                <a className="about__link" href="https://github.com/YOUR-NAME/YOUR-REPO"
                   target="_blank" rel="noopener noreferrer">
                      https://github.com/danielbnhz
                </a>
            </p>
        </section>
    );
}
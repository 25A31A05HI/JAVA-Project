
import os
import requests
import streamlit as st

# Page configuration
st.set_page_config(
    page_title="AI Gift Planner",
    page_icon="🎁",
    layout="centered"
)

# Java backend URL
BACKEND_URL = "https://ai-gift-planner-backend.onrender.com"

# Custom styling
st.markdown("""
<style>
.stApp {
    background: linear-gradient(135deg, #fff7fb, #f4f0ff);
}
.main-title {
    text-align: center;
    color: #743b82;
    font-size: 2.6rem;
    font-weight: 800;
}
.subtitle {
    text-align: center;
    color: #66566b;
    font-size: 1.05rem;
}
.result-card {
    background: white;
    border: 1px solid #eadcf0;
    border-radius: 14px;
    padding: 16px 18px;
    margin: 10px 0;
    color: #392b40;
}
</style>
""", unsafe_allow_html=True)

# Application heading
st.markdown(
    '<div class="main-title">🎁 AI Gift Planner</div>',
    unsafe_allow_html=True
)

st.markdown(
    '<div class="subtitle">Find thoughtful gifts for the people who matter.</div>',
    unsafe_allow_html=True
)

st.write(
    "Tell us about the recipient and discover "
    "gift ideas based on their preferences!"
)

# Gift planner form
with st.form("gift_planner_form"):

    st.subheader("💝 Recipient Details")

    name = st.text_input(
        "Recipient's Name",
        placeholder="Enter recipient's name"
    )

    age = st.number_input(
        "Age",
        min_value=1,
        max_value=120,
        value=22
    )

    relationship = st.selectbox(
        "Your Relationship",
        [
            "Friend",
            "Family Member",
            "Mother",
            "Father",
            "Sibling",
            "Partner",
            "Colleague",
            "Teacher",
            "Other"
        ]
    )

    st.subheader("🎉 Gift Preferences")

    occasion = st.selectbox(
        "Occasion",
        [
            "Birthday",
            "Anniversary",
            "Wedding",
            "Graduation",
            "Festival",
            "Thank You",
            "Housewarming",
            "Just Because",
            "Other"
        ]
    )

    interests = st.text_input(
        "Interests and Hobbies",
        placeholder="Technology, Music, Gaming, Books..."
    )

    budget = st.selectbox(
        "Your Budget (INR)",
        [
            "Under 500",
            "500 - 1000",
            "1000 - 2000",
            "2000 - 5000",
            "5000 - 10000",
            "Above 10000"
        ]
    )

    submitted = st.form_submit_button(
        "✨ Find Gift Ideas",
        use_container_width=True
    )


# Process user input
if submitted:

    if not name.strip():
        st.error("Please enter the recipient's name.")

    else:

        payload = {
            "name": name.strip(),
            "age": str(age),
            "relationship": relationship,
            "occasion": occasion,
            "interests": interests.strip(),
            "budget": budget
        }

        endpoint = f"{BACKEND_URL}/recommend"

        with st.spinner("Finding perfect gift ideas..."):

            try:
                response = requests.post(
                    endpoint,
                    json=payload,
                    timeout=120
                )

                if response.status_code == 200:

                    data = response.json()

                    st.success("Your gift suggestions are ready!")

                    st.subheader(
                        f"🎁 Gift Ideas for {data.get('recipient', name)}"
                    )

                    recommendations = data.get(
                        "recommendations", []
                    )

                    if recommendations:

                        for index, gift in enumerate(
                            recommendations, 1
                        ):

                            st.markdown(
                                f"""
                                <div class="result-card">
                                    <strong>{index}. {gift}</strong>
                                </div>
                                """,
                                unsafe_allow_html=True
                            )

                    else:
                        st.info(
                            "No gift recommendations received."
                        )

                    if data.get("message"):
                        st.caption(data["message"])

                    st.caption(
                        "These are initial recommendations. "
                        "Gemini AI integration will be added next!"
                    )

                else:
                    try:
                        error = response.json().get(
                            "error", response.text
                        )
                    except ValueError:
                        error = response.text

                    st.error(
                        f"Backend Error ({response.status_code}): {error}"
                    )

            except requests.exceptions.ConnectionError:
                st.error(
                    "Unable to connect to the Java backend. "
                    "Please check whether the backend is running."
                )

            except requests.exceptions.Timeout:
                st.error(
                    "The server took too long to respond. "
                    "Please try again."
                )

            except requests.exceptions.RequestException as error:
                st.error(f"Request failed: {error}")

            except ValueError:
                st.error(
                    "Invalid response received from Java backend."
                )


# Footer
st.divider()

st.markdown(
    """
    <div style="text-align:center;color:#786b7d;">
        Made with 💜 for thoughtful gifting
    </div>
    """,
    unsafe_allow_html=True
)

import os
import requests
import streamlit as st

# ---------------------------------------------------------
# AI Gift Planner - Streamlit Frontend
# Connects to the Java GiftPlanner backend.
# ---------------------------------------------------------

st.set_page_config(
    page_title="AI Gift Planner",
    page_icon="🎁",
    layout="centered"
)

# Read the Java backend URL from Streamlit Secrets or
# the JAVA_BACKEND_URL environment variable.
def get_backend_url():
    try:
        secret_url = st.secrets.get("JAVA_BACKEND_URL", "")
    except Exception:
        secret_url = ""

    return secret_url or os.getenv(
        "JAVA_BACKEND_URL",
        "http://localhost:8000"
    )


st.markdown(
    """
    <style>
        .stApp {
            background: linear-gradient(135deg, #fff7fb 0%, #f4f0ff 100%);
        }
        .main-title {
            text-align: center;
            color: #743b82;
            font-size: 2.6rem;
            font-weight: 800;
            margin-bottom: 0.2rem;
        }
        .subtitle {
            text-align: center;
            color: #66566b;
            font-size: 1.05rem;
            margin-bottom: 1.8rem;
        }
        .result-card {
            background: white;
            border: 1px solid #eadcf0;
            border-radius: 14px;
            padding: 16px 18px;
            margin: 10px 0;
            color: #392b40;
            box-shadow: 0 3px 12px rgba(80, 40, 90, 0.06);
        }
    </style>
    """,
    unsafe_allow_html=True
)

st.markdown(
    '<div class="main-title">🎁 AI Gift Planner</div>',
    unsafe_allow_html=True
)
st.markdown(
    '<div class="subtitle">Find thoughtful gifts for the people who matter.</div>',
    unsafe_allow_html=True
)

st.write(
    "Tell us a little about the recipient, and our gift planner "
    "will suggest ideas based on the occasion, interests and budget."
)

with st.form("gift_planner_form"):
    st.subheader("💝 About the recipient")

    name = st.text_input(
        "Recipient's name *",
        placeholder="e.g. Rahul"
    )

    age = st.number_input(
        "Recipient's age *",
        min_value=1,
        max_value=120,
        value=22,
        step=1
    )

    relationship = st.selectbox(
        "Your relationship with them",
        [
            "Friend",
            "Family member",
            "Mother",
            "Father",
            "Sibling",
            "Partner",
            "Colleague",
            "Teacher",
            "Other"
        ]
    )

    st.subheader("🎉 Gift preferences")

    occasion = st.selectbox(
        "Occasion *",
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
        "Interests and hobbies",
        placeholder="e.g. Technology, music, books, gaming"
    )

    budget = st.selectbox(
        "Your budget (INR) *",
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

        backend_url = get_backend_url().rstrip("/")
        endpoint = f"{backend_url}/recommend"

        with st.spinner("Finding gift ideas for you..."):
            try:
                response = requests.post(
                    endpoint,
                    json=payload,
                    timeout=30
                )

                if response.status_code == 200:
                    data = response.json()

                    st.success("Your gift suggestions are ready!")
                    st.subheader(
                        f"🎁 Gift ideas for {data.get('recipient', name)}"
                    )

                    recommendations = data.get("recommendations", [])

                    if recommendations:
                        for index, gift in enumerate(recommendations, 1):
                            st.markdown(
                                f"""
                                <div class="result-card">
                                    <strong>{index}. {gift}</strong>
                                </div>
                                """,
                                unsafe_allow_html=True
                            )
                    else:
                        st.info("The backend did not return any gift ideas.")

                    message = data.get("message")
                    if message:
                        st.caption(message)

                    st.caption(
                        "These are initial suggestions from the Java backend. "
                        "AI-powered recommendations will be added in the next step."
                    )

                else:
                    try:
                        error_message = response.json().get(
                            "error",
                            response.text
                        )
                    except ValueError:
                        error_message = response.text

                    st.error(
                        f"Java backend returned error "
                        f"{response.status_code}: {error_message}"
                    )

            except requests.exceptions.ConnectionError:
                st.error(
                    "Could not connect to the Java backend. "
                    "Make sure GiftPlanner.java is running and that "
                    "JAVA_BACKEND_URL points to the backend server."
                )
            except requests.exceptions.Timeout:
                st.error(
                    "The Java backend took too long to respond. "
                    "Please try again."
                )
            except requests.exceptions.RequestException as error:
                st.error(f"Request failed: {error}")
            except ValueError:
                st.error(
                    "The backend response was not valid JSON. "
                    "Please check the Java backend."
                )


st.divider()
st.markdown(
    "<div style='text-align:center;color:#786b7d;'>"
    "Made with 💜 for thoughtful gifting"
    "</div>",
    unsafe_allow_html=True
)
